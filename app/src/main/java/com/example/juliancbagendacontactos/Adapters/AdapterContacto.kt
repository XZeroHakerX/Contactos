package com.example.juliancbagendacontactos.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.util.Base64
import com.example.juliancbagendacontactos.R
import com.example.juliancbagendacontactos.models.Contacto

class AdapterContacto(
    private var contactos: ArrayList<Contacto>, // Cambié a var para que se pueda actualizar la lista
    private val llamadaClick: (Contacto) -> Unit,
    private val favClick: (Contacto) -> Unit,
    private val menuClick: (Contacto) -> Unit
) : RecyclerView.Adapter<AdapterContacto.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imagen: ImageView = itemView.findViewById(R.id.img_avatar)
        val nombre: TextView = itemView.findViewById(R.id.txt_nombre)
        val numero: TextView = itemView.findViewById(R.id.txt_numero)
        val btnFav: ImageView = itemView.findViewById(R.id.btn_fav)
        val btnEditar: ImageView = itemView.findViewById(R.id.btn_editar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contactos, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contacto = contactos[position]
        if (contacto.nombre?.length!! >= 8) {
            holder.nombre.text = contacto.nombre.substring(0, 6) + "..."
        } else {
            holder.nombre.text = contacto.nombre
        }

        holder.numero.text = contacto.numeroUno.toString()

        // Verificar si hay imagen en Base64, y mostrarla
        if (!contacto.imagen.isNullOrEmpty()) {
            val byteArray = Base64.decode(contacto.imagen, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            holder.imagen.setImageBitmap(bitmap)
        } else {
            holder.imagen.setImageResource(R.drawable.user_pordefecto)
        }

        // Actualizar el icono del favorito según el valor de vip
        actualizarVipIcono(contacto, holder)

        // Listener para favorito
        holder.btnFav.setOnClickListener {
            // Cambiar el estado de VIP
            favClick(contacto) // Actualiza el estado en el adaptador
            // Actualiza el icono del botón de favorito
            actualizarVipIcono(contacto, holder)
        }

        // Listener para editar
        holder.btnEditar.setOnClickListener {
            menuClick(contacto) // Inicia la actividad de edición
        }
    }

    override fun getItemCount(): Int {
        return contactos.size
    }

    // Metodo para actualizar el icono de VIP
    private fun actualizarVipIcono(contacto: Contacto, holder: ViewHolder) {
        if (contacto.vip == true) {
            holder.btnFav.setImageResource(android.R.drawable.btn_star_big_on) // Favorito activado
        } else {
            holder.btnFav.setImageResource(android.R.drawable.btn_star_big_off) // Favorito desactivado
        }
    }

    // Este es el nuevo método que actualiza la lista
    fun updateList(nuevaLista: List<Contacto>) {
        contactos.clear()  // Limpiar la lista actual
        contactos.addAll(nuevaLista)  // Agregar la nueva lista ordenada o filtrada
        notifyDataSetChanged()  // Notificar al adaptador que la lista ha cambiado
    }
}