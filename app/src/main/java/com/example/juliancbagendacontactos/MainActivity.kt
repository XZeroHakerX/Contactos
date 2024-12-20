package com.example.juliancbagendacontactos

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.juliancbagendacontactos.adapters.AdapterContacto
import com.example.juliancbagendacontactos.adapters.AdapterContacto.ViewHolder
import com.example.juliancbagendacontactos.databinding.ActivityMainBinding
import com.example.juliancbagendacontactos.models.Contacto
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var contactosLista: ArrayList<Contacto>
    private lateinit var contactosRecycler: RecyclerView
    private lateinit var contactosAdaptador: AdapterContacto
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configuración para los márgenes del sistema (StatusBar y NavigationBar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Configuración de Firebase
        database = FirebaseDatabase.getInstance().reference

        // Configuración del RecyclerView
        contactosRecycler = binding.recyclerContactos
        contactosRecycler.layoutManager = LinearLayoutManager(this)
        contactosRecycler.setHasFixedSize(true)
        contactosLista = arrayListOf()

        // Adaptador para RecyclerView
        contactosAdaptador = AdapterContacto(contactosLista,
            llamadaClick = { contacto ->
                // Acción al hacer clic en el botón de llamada
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contacto.numeroUno}"))
                startActivity(intent)
            },
            favClick = { contacto ->
                val contactoId = contacto.id
                if (contactoId != null) {
                    val nuevoVip = contacto.vip != true // Cambiar el valor actual
                    database.child("contactos").child(contactoId).child("vip").setValue(nuevoVip)
                        .addOnCompleteListener {
                            // Actualizar el estado en la lista local
                            contacto.vip = nuevoVip
                            // Notificar al adaptador solo para este ítem
                            val index = contactosLista.indexOfFirst { it.id == contacto.id }
                            if (index != -1) {
                                contactosAdaptador.notifyItemChanged(index)
                            }
                            // Ordenar la lista después de cambiar el estado de favorito
                            ordenarContactos()
                        }
                }
            },
            menuClick = { contacto ->
                // Acción al hacer clic en el botón de más opciones
                // Ejemplo: abrir un diálogo para editar o eliminar contacto
                val intent = Intent(this, EditarContactoActivity::class.java)
                intent.putExtra("contacto", contacto)  // Envías el contacto a la actividad de edición
                startActivityForResult(intent, REQUEST_CODE_EDITAR_CONTACTO)  // Inicia la actividad y espera el resultado
            })
        contactosRecycler.adapter = contactosAdaptador

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false // No mover los elementos
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val contacto = contactosLista[position]

                if (direction == ItemTouchHelper.LEFT) {
                    // Deslizar hacia la izquierda para enviar un mensaje
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${contacto.numeroUno}"))
                    intent.putExtra("sms_body", "¡Hola! ¿Cómo estás?")
                    startActivity(intent)
                } else if (direction == ItemTouchHelper.RIGHT) {
                    // Deslizar hacia la derecha para realizar una llamada
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contacto.numeroUno}"))
                    startActivity(intent)
                }

                // Asegúrate de que el RecyclerView recargue los datos
                contactosAdaptador.notifyItemChanged(position)
            }

            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                                     dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

                val iconWidth = 200f // El tamaño del ícono que dibujaremos
                val backgroundPaint = Paint()

                // Si se desliza hacia la izquierda (mensaje)
                if (dX < 0) {
                    // Color azul para mensaje
                    backgroundPaint.color = Color.BLUE

                    // Dibujar el fondo azul
                    c.drawRect(viewHolder.itemView.right.toFloat() + dX, viewHolder.itemView.top.toFloat(),
                        viewHolder.itemView.right.toFloat(), viewHolder.itemView.bottom.toFloat(), backgroundPaint)

                    // Dibujar icono de mensaje
                    val icon = ContextCompat.getDrawable(applicationContext, R.drawable.ic_message) // Asegúrate de tener un ícono de mensaje
                    icon?.setBounds(
                        viewHolder.itemView.right - iconWidth.toInt(),
                        viewHolder.itemView.top + (viewHolder.itemView.height / 2 - iconWidth.toInt() / 2),
                        viewHolder.itemView.right,
                        viewHolder.itemView.top + (viewHolder.itemView.height / 2 + iconWidth.toInt() / 2)
                    )
                    icon?.draw(c)

                }
                // Si se desliza hacia la derecha (llamada)
                else if (dX > 0) {
                    // Color verde para llamada
                    backgroundPaint.color = Color.GREEN

                    // Dibujar el fondo verde
                    c.drawRect(viewHolder.itemView.left.toFloat(), viewHolder.itemView.top.toFloat(),
                        viewHolder.itemView.left.toFloat() + dX, viewHolder.itemView.bottom.toFloat(), backgroundPaint)

                    // Dibujar icono de llamada
                    val icon = ContextCompat.getDrawable(applicationContext, R.drawable.ic_call) // Asegúrate de tener un ícono de llamada
                    icon?.setBounds(
                        viewHolder.itemView.left,
                        viewHolder.itemView.top + (viewHolder.itemView.height / 2 - iconWidth.toInt() / 2),
                        viewHolder.itemView.left + iconWidth.toInt(),
                        viewHolder.itemView.top + (viewHolder.itemView.height / 2 + iconWidth.toInt() / 2)
                    )
                    icon?.draw(c)
                }
            }
        })

        itemTouchHelper.attachToRecyclerView(contactosRecycler)
        // Cargar los datos de Firebase
        cargarDatos()

        // Configurar el botón flotante para agregar un contacto
        binding.btnAniadir.setOnClickListener {
            // Acción al hacer clic en el botón de añadir un contacto
            val intent = Intent(this, AgregarContactoActivity::class.java)
            startActivity(intent)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_EDITAR_CONTACTO && resultCode == Activity.RESULT_OK) {
            val eliminado = data?.getBooleanExtra("contactoEliminado", false) ?: false
            if (eliminado) {
                cargarDatos() // Recargar la lista desde Firebase
            }
        }
    }


    // Función para cargar los datos de Firebase
    private fun cargarDatos() {
        // Aquí iría tu lógica para cargar la lista de contactos desde Firebase
        // Asegúrate de ordenar los contactos con los favoritos primero
        contactosLista.clear()  // Limpiar la lista
        database.child("contactos").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val contacto = snapshot.getValue(Contacto::class.java)
                if (contactosLista.none { it.id == contacto!!.id }) {
                    contactosLista.add(contacto!!)
                    ordenarContactos()  // Ordenar antes de actualizar el RecyclerView
                    contactosAdaptador.notifyItemInserted(contactosLista.size - 1)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val contacto = snapshot.getValue(Contacto::class.java)
                if (contacto != null) {
                    val index = contactosLista.indexOfFirst { it.id == contacto.id }
                    if (index != -1) {
                        contactosLista[index] = contacto
                        ordenarContactos()  // Asegúrate de ordenar los contactos cada vez que se actualiza
                        contactosAdaptador.notifyItemChanged(index)
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val contacto = snapshot.getValue(Contacto::class.java)
                if (contacto != null) {
                    val index = contactosLista.indexOfFirst { it.id == contacto.id }
                    if (index != -1) {
                        contactosLista.removeAt(index)
                        contactosAdaptador.notifyItemRemoved(index)
                        // Si eliminamos un contacto, recargamos la lista para actualizar los favoritos
                        ordenarContactos()
                    }
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Manejo del cambio de posición (opcional)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error al cargar los contactos: ${error.message}")
            }
        })
    }

    private fun ordenarContactos() {
        // Ordenar los contactos, los favoritos primero
        contactosLista.sortWith(compareByDescending { it.vip })
        contactosAdaptador.notifyDataSetChanged()
    }
    companion object {
        private const val REQUEST_CODE_EDITAR_CONTACTO = 1001
    }
}