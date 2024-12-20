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

    // Referencia a la base de datos de Firebase y Storage
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference

    // Vista del layout
    private lateinit var binding: ActivityAgregarContactoBinding

    // Variable para almacenar la imagen seleccionada
    private var selectedImageUri: Uri? = null

    // Variable para lanzar el selector de imágenes
    private lateinit var getContent: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa ViewBinding
        binding = ActivityAgregarContactoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa Firebase Realtime Database y Firebase Storage
        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference

        // Configura el botón de guardar
        binding.btnGuardar.setOnClickListener {
            guardarContacto()
        }

        // Configura el botón de cancelar
        binding.btnCancelar.setOnClickListener {
            finish() // Cierra la actividad y vuelve a la anterior
        }

        // Configura la imagen como botón para seleccionar foto
        binding.imgFotoContacto.setOnClickListener {
            abrirSelectorDeImagen()
        }

        // Configura el selector de fecha de cumpleaños
        binding.etFechaCumpleanos.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Selecciona tu fecha de nacimiento")
                .build()

            datePicker.addOnPositiveButtonClickListener {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.etFechaCumpleanos.setText(sdf.format(Date(it)))
            }

            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }

        // Registrar el launcher para la selección de imagen
        getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            // Si se selecciona una imagen, actualizar la vista previa
            uri?.let {
                selectedImageUri = it
                binding.imgFotoContacto.setImageURI(it)  // Previsualiza la imagen seleccionada
            }
        }
    }

    // Abre el selector de imagen para elegir una foto
    private fun abrirSelectorDeImagen() {
        // Lanza el selector de imágenes para elegir una imagen de tipo "image/*"
        getContent.launch("image/*")
    }

    private fun guardarContacto() {
        // Obtener los valores de los campos de entrada usando ViewBinding
        val nombre = binding.etNombre.text.toString().trim()
        val apellidoUno = binding.etApellido1.text.toString().trim()
        val apellidoDos = binding.etApellido2.text.toString().trim()
        val telefono1 = binding.etTelefono1.text.toString().toIntOrNull()
        val telefono2 = binding.etTelefono2.text.toString().toIntOrNull()
        val email = binding.etEmail.text.toString().trim()
        val fechaNacimiento = binding.etFechaCumpleanos.text.toString().trim()
        val mensajePersonal = binding.etMensajePersonal.text.toString().trim()

        // Asegurarse de que los campos de texto no estén vacíos
        if (nombre.isEmpty() || apellidoUno.isEmpty() || apellidoDos.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        // Convertir la imagen seleccionada en un Bitmap si está presente
        val imagenBitmap = selectedImageUri?.let {
            try {
                // Usar ContentResolver para obtener el Bitmap de la URI
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        // Convertir la imagen a un String Base64
        val imagenBase64 = imagenBitmap?.let {
            val byteArray = bitmapToByteArray(it)
            byteArrayToBase64(byteArray)
        }

        // Convertir la fecha de nacimiento a Date
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val nacimientoDate = try {
            sdf.parse(fechaNacimiento)
        } catch (e: ParseException) {
            null
        }

        // Convertir la fecha a milisegundos (Long) para almacenar en Firebase
        val nacimientoMillis = nacimientoDate?.time ?: -1L

        // Crear un objeto de contacto con los valores obtenidos
        val contacto = Contacto(
            id = null, // Generar un ID único
            vip = binding.switchVip.isChecked, // Valor de VIP desde el Switch
            imagen = imagenBase64, // Asignar la cadena Base64 de la imagen
            nombre = nombre,
            apellidoUno = apellidoUno,
            apellidoDos = apellidoDos,
            numeroUno = telefono1,
            numeroDos = telefono2,
            email = email,
            nacimiento = nacimientoMillis, // Guardar la fecha como milisegundos (Long)
            mensajePersonal = mensajePersonal
        )

// Guardar el objeto de contacto en la base de datos de Firebase
        val contactoId = database.child("contactos").push().key!!

        contacto.id = contactoId
// Asegúrate de que todos los campos estén siendo enviados a Firebase correctamente
        val contactoMap = mapOf(
            "id" to contacto.id,
            "vip" to contacto.vip,  // Guarda correctamente el estado de favorito
            "imagen" to (contacto.imagen),
            "nombre" to contacto.nombre,
            "apellidoUno" to contacto.apellidoUno,
            "apellidoDos" to contacto.apellidoDos,
            "numeroUno" to contacto.numeroUno,
            "numeroDos" to contacto.numeroDos,
            "email" to contacto.email,
            "nacimiento" to contacto.nacimiento, // Ahora es un Long (milisegundos)
            "mensajePersonal" to contacto.mensajePersonal
        )

// Usamos setValue para guardar el contacto
        database.child("contactos").child(contactoId).setValue(contactoMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Contacto guardado exitosamente", Toast.LENGTH_SHORT).show()
                finish() // Cerrar la actividad después de guardar el contacto
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al guardar el contacto: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Convertir el Bitmap en un ByteArray
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    // Convertir el ByteArray en una cadena Base64
    private fun byteArrayToBase64(byteArray: ByteArray): String {
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}
