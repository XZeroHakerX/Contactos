package com.example.juliancbagendacontactos.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.util.Base64
import com.example.juliancbagendacontactos.R
import com.example.juliancbagendacontactos.models.Contacto


//Clase Adapter para los contactos y el recyclerView, ArrayList de contactos, y metodo favClick y menuClick
// para los botones de los contactos:
class AdapterContacto(
    private var contactos: ArrayList<Contacto>, // Cambié a var para que se pueda actualizar la lista
    private val favClick: (Contacto) -> Unit,
    private val menuClick: (Contacto) -> Unit
) : RecyclerView.Adapter<AdapterContacto.ViewHolder>() {


    //ViewHolder para hacer la referencia con los componentes:
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imagen: ImageView = itemView.findViewById(R.id.img_avatar)
        val nombre: TextView = itemView.findViewById(R.id.txt_nombre)
        val numero: TextView = itemView.findViewById(R.id.txt_numero)
        val btnFav: ImageView = itemView.findViewById(R.id.btn_fav)
        val btnEditar: ImageView = itemView.findViewById(R.id.btn_editar)
    }


    //Funcion onCreate, inlfamos el layout:
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contactos, parent, false)
        return ViewHolder(view)
    }



    //Funcion para ir mostrando y como los elementos del recycler:
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

    //Recuperamos el contacto pertinente en el recorrido:
        val contacto = contactos[position]

    //Si el nombnre es mas largo de 8 caracteres se acorta con puntos suspensivos:
        if (contacto.nombre?.length!! >= 8) {
            holder.nombre.text = contacto.nombre.substring(0, 6) + "..."
        } else {
            holder.nombre.text = contacto.nombre
        }

    //Numero principal de telefono, mostramos solo el telefono uno dentro de la
    // cardView en el recyclerVIew
        holder.numero.text = contacto.numeroUno.toString()

    // Verificamos si hay imagen asociada al contacto en Base64, y mostrarla y pasamos a
    // mostrarla haciendo una decodificacion de base64 a byteArray, y despues a bitmap,
    // asi nos aseguramos de que firebase haga un buen guardado de la imagen:
        if (!contacto.imagen.isNullOrEmpty()) {
            val byteArray = Base64.decode(contacto.imagen, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            holder.imagen.setImageBitmap(bitmap)
        } else {
            holder.imagen.setImageResource(R.drawable.user_pordefecto)
        }

    // Actualizar el icono del favorito según el valor de vip una vez que va cargando
    // los contactos, para su ordenacion pòsterior:
        actualizarVipIcono(contacto, holder)

    // Listener para boton favorito
        holder.btnFav.setOnClickListener {

        // Cambiar el estado de VIP
            favClick(contacto) // Actualiza el estado en el adaptador

        // Actualiza el icono del botón de favorito
            actualizarVipIcono(contacto, holder)
        }


    // Listener para editar
        holder.btnEditar.setOnClickListener {

        // Ejecutamos la actividad de editar contacto, pasandole el contacto a editar
            menuClick(contacto)
        }
    }



    //Funcion necesaria para el recorrido del recycler:
    override fun getItemCount(): Int {
        return contactos.size
    }



    // Metodo para actualizar el icono de VIP, se llama cuando se pulsa la imagen de la
    // estrella y comprueba si el contacto es vip o no y cambia la imagen a razon:
    private fun actualizarVipIcono(contacto: Contacto, holder: ViewHolder) {
        if (contacto.vip == true) {
            holder.btnFav.setImageResource(android.R.drawable.btn_star_big_on) // Favorito activado
        } else {
            holder.btnFav.setImageResource(android.R.drawable.btn_star_big_off) // Favorito desactivado
        }
    }



// Este es el metodo que actualiza la lista, lo llamamos cuando
// hacemos cambios y queremos refrescar la lista local para que se
// sincronice, usada para los filtros y busqueda:
    fun updateList(nuevaLista: List<Contacto>) {
        contactos.clear()
        contactos.addAll(nuevaLista)
        notifyDataSetChanged()
    }
}