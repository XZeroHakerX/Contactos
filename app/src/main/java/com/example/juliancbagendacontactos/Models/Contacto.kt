package com.example.juliancbagendacontactos.models



import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Parcel
import android.os.Parcelable
import android.util.Base64
import java.io.ByteArrayOutputStream

//DataClass de Contacto para defenir atributos y
//demas necesario para el recycler:
data class Contacto(
    var id: String? = null,
    var vip: Boolean? = null,
    val imagen: String? = null,
    val nombre: String? = null,
    val apellidoUno: String? = null,
    val apellidoDos: String? = null,
    val numeroUno: Int? = null,
    val numeroDos: Int? = null,
    val email: String? = null,
    var nacimiento: Long? = null,
    val mensajePersonal: String? = null
) : Parcelable {

    //Constructor para crear el objeto desde el Parcel, utrilizamos la clase Parcelable de una forma
    //similar al serializable de java:
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        //La imagen se lee y se guarda en firebase como un string  de Base64
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readString(),
        parcel.readLong(),
        parcel.readString()
    )

    //Estos dos metodos hacen la conversion de la foto para que se maneje y guarde correctamente:
    //Metodo para convertir el Bitmap en un ByteArray
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    //Metodo para convertir un ByteArray a una cadena Base64
    private fun byteArrayToBase64(byteArray: ByteArray): String {
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    //Metodo companion para convertir un String de vuelta a Bitmap, aqui se hace el procedimiento
    //contrario al anterior:
    companion object {

        fun base64ToBitmap(base64String: String?): Bitmap? {
            return base64String?.let {
                val decodedByteArray = Base64.decode(it, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedByteArray, 0, decodedByteArray.size)
            }
        }

        //Creamos el CREATOR para el Parcelable
        @JvmField
        val CREATOR: Parcelable.Creator<Contacto> = object : Parcelable.Creator<Contacto> {
            override fun createFromParcel(parcel: Parcel): Contacto {
                return Contacto(parcel)
            }

            override fun newArray(size: Int): Array<Contacto?> {
                return arrayOfNulls(size)
            }
        }
    }

    //Metodo para escribir los datos del objeto en un Parcel
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeValue(vip)
        //Convertir la imagen a un String Base64 y la escribimos
        val imageBase64 = imagen
        parcel.writeString(imageBase64)
        parcel.writeString(nombre)
        parcel.writeString(apellidoUno)
        parcel.writeString(apellidoDos)
        parcel.writeValue(numeroUno)
        parcel.writeValue(numeroDos)
        parcel.writeString(email)
        parcel.writeLong(nacimiento ?: -1)
        parcel.writeString(mensajePersonal)
    }

    override fun describeContents(): Int {
        return 0
    }
}