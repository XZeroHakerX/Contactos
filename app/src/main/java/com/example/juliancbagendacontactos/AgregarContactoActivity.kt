package com.example.juliancbagendacontactos

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.juliancbagendacontactos.models.Contacto
import com.example.juliancbagendacontactos.databinding.ActivityAgregarContactoBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import android.util.Base64
import java.util.*

class AgregarContactoActivity : AppCompatActivity() {

    //Referencias a Firebase Realtime Database y Storage para guardar la foto
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference

    //Vista del layout con binding
    private lateinit var binding: ActivityAgregarContactoBinding

    //Variable para almacenar la URI de la imagen seleccionada
    private var selectedImageUri: Uri? = null

    //Lanzador del selector de imagenes, elegiremos la foto a subir
    private lateinit var getContent: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Inicializacimos el ViewBinding
        binding = ActivityAgregarContactoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Inicializacimos Firebase
        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference

        //Configuracion de los listeners:
        configurarListeners()

        //Hacemos el registro del lanzador para el selector de imágenes
        getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                //Aqui mandamos para previsualizar la imagen antes de guardar:
                binding.imgFotoContacto.setImageURI(it)
            }
        }
    }

    //Listeners:
    private fun configurarListeners() {
        //Boton de guardar
        binding.btnGuardar.setOnClickListener {
            guardarContacto()
        }

        //Boton de cancelar
        binding.btnCancelar.setOnClickListener {
            finish()  // Cierra la actividad
        }

        //Configuracion del selector de imagen
        binding.imgFotoContacto.setOnClickListener {
            abrirSelectorDeImagen()
        }

        //Configuracion del selector de fecha de cumpleaños
        binding.etFechaCumpleanos.setOnClickListener {
            //Utilizo MaterialDatePicker por ser mas recomendable:
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Selecciona tu fecha de nacimiento")
                .build()
            //Las fechas las guardamos en firebase como un long de milisegundos
            datePicker.addOnPositiveButtonClickListener { dateMillis ->
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.etFechaCumpleanos.setText(sdf.format(Date(dateMillis)))
            }

            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }
    }

    //Metodo que lanza el selector des la ruta que se indica:
    private fun abrirSelectorDeImagen() {
        getContent.launch("image/*")
    }

    //Metodo para guardar un contacto nuevo:
    private fun guardarContacto() {
        //Validacion de campos obligatorios minimos:
        val nombre = binding.etNombre.text.toString().trim()
        val telefono1S = binding.etTelefono1.text.toString().trim()

        //Error de datos minimos:

        if (nombre.isEmpty()) {
            binding.etNombre.error = "Este campo es obligatorio"
        }

        if (telefono1S.toString().isEmpty()) {
            binding.etTelefono1.error = "Este campo es obligatorio"
        }

        if (nombre.isEmpty() || telefono1S.toString().isEmpty()) {
            Toast.makeText(
                this,
                "Por favor, completa todos los campos obligatorios",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val telefono1 = telefono1S.toIntOrNull()

        //Obtener otros campos opcionales
        val apellidoUno = binding.etApellido1.text.toString().trim()
        val apellidoDos = binding.etApellido2.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val telefono2 = binding.etTelefono2.text.toString().toIntOrNull()
        val fechaNacimiento = binding.etFechaCumpleanos.text.toString().trim()
        val mensajePersonal = binding.etMensajePersonal.text.toString().trim()


        //Convertir fecha de nacimiento a milisegundos
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val nacimientoMillis = try {
            sdf.parse(fechaNacimiento)?.time ?: -1L
        } catch (e: ParseException) {
            -1L
        }

        //Convertir imagen seleccionada a Base64
        val imagenBase64 = selectedImageUri?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
            bitmapToBase64(bitmap)
        }

        //Generar ID unico para cada contacto
        val contactoId = database.child("contactos").push().key ?: return

        //Crear el objeto Contacto que vamos a guardar en firebase:
        val contacto = Contacto(
            id = contactoId,
            vip = binding.switchVip.isChecked,
            imagen = imagenBase64,
            nombre = nombre,
            apellidoUno = apellidoUno,
            apellidoDos = apellidoDos,
            numeroUno = telefono1,
            numeroDos = telefono2,
            email = email,
            nacimiento = if (nacimientoMillis != -1L) nacimientoMillis else null,
            mensajePersonal = mensajePersonal
        )

        //Guardamos en Firebase:
        database.child("contactos").child(contactoId).setValue(contacto)
            .addOnSuccessListener {
                //Mostramos tostada de confirmacion:
                Toast.makeText(this, "Contacto guardado exitosamente", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar contacto: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    //Metodo que utlizo para pasar la imagen a Base64:
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}

