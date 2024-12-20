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

    // Referencias a Firebase Realtime Database y Storage
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference

    // Vista del layout
    private lateinit var binding: ActivityAgregarContactoBinding

    // Variable para almacenar la URI de la imagen seleccionada
    private var selectedImageUri: Uri? = null

    // Lanzador del selector de imágenes
    private lateinit var getContent: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicialización de ViewBinding
        binding = ActivityAgregarContactoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicialización de Firebase
        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference

        // Configuración de botones y eventos
        configurarListeners()

        // Registro del lanzador para el selector de imágenes
        getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                binding.imgFotoContacto.setImageURI(it)  // Previsualización de la imagen seleccionada
            }
        }
    }

    private fun configurarListeners() {
        // Configurar botón de guardar
        binding.btnGuardar.setOnClickListener {
            guardarContacto()
        }

        // Configurar botón de cancelar
        binding.btnCancelar.setOnClickListener {
            finish()  // Cierra la actividad
        }

        // Configurar selector de imagen
        binding.imgFotoContacto.setOnClickListener {
            abrirSelectorDeImagen()
        }

        // Configurar selector de fecha de cumpleaños
        binding.etFechaCumpleanos.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Selecciona tu fecha de nacimiento")
                .build()

            datePicker.addOnPositiveButtonClickListener { dateMillis ->
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.etFechaCumpleanos.setText(sdf.format(Date(dateMillis)))
            }

            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }
    }

    private fun abrirSelectorDeImagen() {
        getContent.launch("image/*")
    }

    private fun guardarContacto() {
        // Validación de campos obligatorios
        val nombre = binding.etNombre.text.toString().trim()
        val apellidoUno = binding.etApellido1.text.toString().trim()
        val apellidoDos = binding.etApellido2.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()

        if (nombre.isEmpty() || apellidoUno.isEmpty() || apellidoDos.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener otros campos opcionales
        val telefono1 = binding.etTelefono1.text.toString().toIntOrNull()
        val telefono2 = binding.etTelefono2.text.toString().toIntOrNull()
        val fechaNacimiento = binding.etFechaCumpleanos.text.toString().trim()
        val mensajePersonal = binding.etMensajePersonal.text.toString().trim()

        // Convertir fecha de nacimiento a milisegundos
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val nacimientoMillis = try {
            sdf.parse(fechaNacimiento)?.time ?: -1L
        } catch (e: ParseException) {
            -1L
        }

        // Convertir imagen seleccionada a Base64
        val imagenBase64 = selectedImageUri?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
            bitmapToBase64(bitmap)
        }

        // Generar ID único para el contacto
        val contactoId = database.child("contactos").push().key ?: return

        // Crear el objeto Contacto
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

        // Guardar en Firebase
        database.child("contactos").child(contactoId).setValue(contacto)
            .addOnSuccessListener {
                Toast.makeText(this, "Contacto guardado exitosamente", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar contacto: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}

