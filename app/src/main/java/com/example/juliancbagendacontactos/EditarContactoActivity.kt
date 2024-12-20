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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.collections.HashMap

class EditarContactoActivity : AppCompatActivity() {

    //Variables para el binding y para el id sobre el que trabajaremos:
    private lateinit var binding: ActivityEditarContactoBinding
    private var contactoId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditarContactoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Recuperamos el Id desde el Intent
        contactoId = intent.getStringExtra("contactoId")
        if (contactoId == null) {
            //Error si por lo que sea el id no se puede recuperar:
            Toast.makeText(this, "Error al cargar contacto", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        //Cargamos datos del contacto desde irebase a partir del id:
        val database = FirebaseDatabase.getInstance().reference.child("contactos").child(contactoId!!)


        database.addListenerForSingleValueEvent(object : ValueEventListener {
            //Con este listener cargamos los datos del contacto seleccionado en los campos,
            //si no hay contacto, genera una tostada y sale de la actividad:
            override fun onDataChange(snapshot: DataSnapshot) {
                val contacto = snapshot.getValue(Contacto::class.java)
                if (contacto != null) {
                    //Si existe, carga los datos:
                    cargarDatosEnCampos(contacto)
                } else {
                    //Sino lanza error y vuelve:
                    Toast.makeText(this@EditarContactoActivity, "Contacto no encontrado", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            //Error de carga de datos, por si fallara cuando va a leer de firebase:
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@EditarContactoActivity, "Error al cargar datos", Toast.LENGTH_SHORT).show()
                finish()
            }
        })

        //Configuramos los listeners de los botones:
        configurarListeners()
    }


    //Listeners:
    private fun configurarListeners() {
        //Configuramos selector de fecha si pulsamos boton de cambiar fecha:
        binding.btnFechaNacimiento.setOnClickListener {
            mostrarSelectorFecha()
        }

        //Configuramos boton de guardar cambios
        binding.btnGuardar.setOnClickListener {
            guardarCambios()
        }

        //Configuramos boton de eliminar contacto
        binding.btnEliminar.setOnClickListener {
            eliminarContacto()
        }

        //Configuramos boton de volver
        binding.btnVolver.setOnClickListener {
            finish()
        }

        //Configuramos para cambiar imagen al hacer click en la propia ImageView
        binding.imageContacto.setOnClickListener {
            seleccionarImagen()
        }
    }


    //Metodo para cargar los datos en los diferentes editText, para su posterior modificacion:
    private fun cargarDatosEnCampos(contacto: Contacto) {
        with(binding) {
            inputNombre.setText(contacto.nombre)
            inputApellidoUno.setText(contacto.apellidoUno)
            inputApellidoDos.setText(contacto.apellidoDos)
            inputNumeroUno.setText(contacto.numeroUno?.toString())
            inputNumeroDos.setText(contacto.numeroDos?.toString())
            inputEmail.setText(contacto.email)
            inputMensajePersonal.setText(contacto.mensajePersonal)

            //Cargamos la fecha haciendo las conversiones necesarias:
            contacto.nacimiento?.let { fecha ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = fecha
                val fechaTexto = "${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.YEAR)}"
                btnFechaNacimiento.text = fechaTexto
            }

            //Cargamos la imagen haciendo las modificaciones necesarias de Base64 a Bitmap:
            contacto.imagen?.let { base64String ->
                val byteArray = Base64.decode(base64String, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                imageContacto.setImageBitmap(bitmap)
            }
        }
    }

    //Metodo para la seleccion de una nueva fecha a traves de un nuevo DatePickerDialog:
    private fun mostrarSelectorFecha() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val fechaTexto = "$dayOfMonth/${month + 1}/$year"
                binding.btnFechaNacimiento.text = fechaTexto
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    //Metodo para la seleccion de una nueva imagen:
    private fun seleccionarImagen() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_CODE_IMAGE_PICKER)
    }


    //Metodo para guardar los cambios realizados:
    private fun guardarCambios() {
        //Si el contacto es null, tira error antes de continuar:
        if (contactoId == null) {
            Toast.makeText(this, "Error al guardar los cambios.", Toast.LENGTH_SHORT).show()
            return
        }

        //Recuperamos la imagen y la pasamos a ByteArray:
        val byteArray = (binding.imageContacto.drawable as? BitmapDrawable)?.bitmap?.let { bitmap ->
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            baos.toByteArray()
        }

        //Y luego Base64 para firebase
        val imagenBase64 = byteArray?.let {
            Base64.encodeToString(it, Base64.DEFAULT)
        }

        //Nuevo contacto con los datos modificados, teniendo en cuenta que la id se mantenga y sea la misma:
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
            vip = false // Cambiar según tu lógica
        )

        //Si es correcto y no falla nada, guardamos en firebase:
        val database = FirebaseDatabase.getInstance().reference.child("contactos")
        database.child(contactoId!!).setValue(nuevoContacto)
            .addOnSuccessListener {
                Toast.makeText(this, "Contacto actualizado", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar contacto", Toast.LENGTH_SHORT).show()
            }
    }


    //Metodo para la eliminacion de un contacto:
    private fun eliminarContacto() {
        //Si no existe, falla y termina la ejecucion del metodo:
        if (contactoId == null) {
            Toast.makeText(this, "Error: No se puede eliminar el contacto.", Toast.LENGTH_SHORT).show()
            return
        }

        //Cuadro de dialogo de confirmacion para la eliminación
        AlertDialog.Builder(this)
            .setTitle("Eliminar Contacto")
            .setMessage("¿Estás seguro de que deseas eliminar este contacto? Esta acción no se puede deshacer.")
            .setPositiveButton("Sí") { _, _ ->
                //Si el usuario confirma, pasaremos a la eliminacion del contacto:
                val database = FirebaseDatabase.getInstance().reference.child("contactos")
                database.child(contactoId!!).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Contacto eliminado", Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al eliminar contacto", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    //Metodo que utiliza la actividad que llama a esta propia, para hace un buen control de
    //las eliminaciones, principalmente con el ultimo elemento:
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_IMAGE_PICKER && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                binding.imageContacto.setImageBitmap(bitmap)
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_IMAGE_PICKER = 101
    }
}
