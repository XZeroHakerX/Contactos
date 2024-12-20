package com.example.juliancbagendacontactos


import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.juliancbagendacontactos.databinding.ActivityEditarContactoBinding
import com.example.juliancbagendacontactos.models.Contacto
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.collections.HashMap

class EditarContactoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditarContactoBinding
    private var contactoId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditarContactoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Recuperar datos del intent
        val contacto = intent.getParcelableExtra<Contacto>("contacto")
        if (contacto == null) {
            Toast.makeText(this, "Error al cargar contacto", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        contactoId = contacto.id

        // Cargar datos en los campos
        cargarDatosEnCampos(contacto)

        // Configurar selector de fecha
        binding.btnFechaNacimiento.setOnClickListener {
            mostrarSelectorFecha(contacto)
        }

        // Botón de guardar cambios
        binding.btnGuardar.setOnClickListener {
            guardarCambios(contacto)
        }

        // Botón de eliminar contacto
        binding.btnEliminar.setOnClickListener {
            eliminarContacto()
        }

        // Botón de volver
        binding.btnVolver.setOnClickListener {
            finish()
        }

        // Cambiar imagen al hacer clic en la ImageView
        binding.imageContacto.setOnClickListener {
            seleccionarImagen()
        }
    }

    private fun cargarDatosEnCampos(contacto: Contacto) {
        with(binding) {
            inputNombre.setText(contacto.nombre)
            inputApellidoUno.setText(contacto.apellidoUno)
            inputApellidoDos.setText(contacto.apellidoDos)
            inputNumeroUno.setText(contacto.numeroUno?.toString())
            inputNumeroDos.setText(contacto.numeroDos?.toString())
            inputEmail.setText(contacto.email)
            inputMensajePersonal.setText(contacto.mensajePersonal)

            contacto.nacimiento?.let { fecha ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = fecha
                val fechaTexto = "${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.YEAR)}"
                btnFechaNacimiento.text = fechaTexto
            }

            contacto.imagen?.let { base64String ->
                val byteArray = Base64.decode(base64String, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                imageContacto.setImageBitmap(bitmap)
            }
        }
    }

    private fun mostrarSelectorFecha(contacto: Contacto) {
        val calendar = Calendar.getInstance()
        contacto.nacimiento?.let {
            calendar.timeInMillis = it
        }
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val fechaTexto = "$dayOfMonth/${month + 1}/$year"
                binding.btnFechaNacimiento.text = fechaTexto
                contacto.nacimiento = GregorianCalendar(year, month, dayOfMonth).timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun seleccionarImagen() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_CODE_IMAGE_PICKER)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_IMAGE_PICKER && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                binding.imageContacto.setImageBitmap(bitmap)
            }
        }
    }

    private fun guardarCambios(contacto: Contacto) {
        val byteArray = (binding.imageContacto.drawable as? BitmapDrawable)?.bitmap?.let { bitmap ->
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            baos.toByteArray()
        }

        val imagenBase64 = byteArray?.let {
            Base64.encodeToString(it, Base64.DEFAULT)
        }

        val nuevoContacto = Contacto(
            id = contactoId,
            nombre = binding.inputNombre.text.toString(),
            apellidoUno = binding.inputApellidoUno.text.toString(),
            apellidoDos = binding.inputApellidoDos.text.toString(),
            numeroUno = binding.inputNumeroUno.text.toString().toIntOrNull(),
            numeroDos = binding.inputNumeroDos.text.toString().toIntOrNull(),
            email = binding.inputEmail.text.toString(),
            mensajePersonal = binding.inputMensajePersonal.text.toString(),
            nacimiento = binding.btnFechaNacimiento.text?.toString()?.let {
                val parts = it.split("/")
                GregorianCalendar(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt()).timeInMillis
            },
            imagen = imagenBase64,
            vip = contacto.vip
        )

        val database = FirebaseDatabase.getInstance().reference.child("contactos")
        database.child(contactoId!!).setValue(nuevoContacto)
            .addOnSuccessListener {
                Toast.makeText(this, "Contacto actualizado", Toast.LENGTH_SHORT).show()
                onBackPressedDispatcher.onBackPressed()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar contacto", Toast.LENGTH_SHORT).show()
            }
    }

    private fun eliminarContacto() {
        if (contactoId == null) {
            Toast.makeText(this, "Error: No se puede eliminar el contacto.", Toast.LENGTH_SHORT).show()
            return
        }

        // Mostrar cuadro de diálogo de confirmación
        AlertDialog.Builder(this)
            .setTitle("Eliminar Contacto")
            .setMessage("¿Estás seguro de que deseas eliminar este contacto? Esta acción no se puede deshacer.")
            .setPositiveButton("Sí") { _, _ ->

                // Proceder con la eliminación
                val database = FirebaseDatabase.getInstance().reference.child("contactos")
                database.child(contactoId!!).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Contacto eliminado", Toast.LENGTH_SHORT).show()
                        val resultIntent = Intent()
                        resultIntent.putExtra("contactoEliminado", true)
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al eliminar contacto", Toast.LENGTH_SHORT).show()
                    }

            }
            .setNegativeButton("No") { dialog, _ ->
                // Cerrar el cuadro de diálogo
                dialog.dismiss()
            }
            .create()
            .show()
    }

    companion object {
        private const val REQUEST_CODE_IMAGE_PICKER = 101
    }
}