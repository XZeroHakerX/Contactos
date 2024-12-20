package com.example.juliancbagendacontactos

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*
import androidx.appcompat.widget.SearchView

class MainActivity : AppCompatActivity() {

    //Variables para el uso del programa, variables para binding, para la conexion a base de datos,
    //para el recyclerView y su Adaptador, una lista provisional de los contactos recuperados de la
    //base de datos e inicializacion de los componentes de busqueda:
    private lateinit var binding: ActivityMainBinding
    private lateinit var contactosLista: ArrayList<Contacto>
    private lateinit var contactosRecycler: RecyclerView
    private lateinit var contactosAdaptador: AdapterContacto
    private lateinit var database: DatabaseReference
    private lateinit var searchView: SearchView
    private lateinit var btnBuscar: FloatingActionButton
    private lateinit var contactosListaOriginal: ArrayList<Contacto>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Configuración para los márgenes del sistema, por si incluimos barras de navegacion, etc
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        //Busqueda tradicional de componentes, usados para el buscador:
        searchView = findViewById(R.id.searchView)
        btnBuscar = findViewById(R.id.btnBuscar)

        //Configuracion de Firebase:
        database = FirebaseDatabase.getInstance().reference

        //Configuracion del RecyclerView
        contactosRecycler = binding.recyclerContactos
        contactosRecycler.layoutManager = LinearLayoutManager(this)
        contactosRecycler.setHasFixedSize(true)
        contactosLista = arrayListOf()
        //Hacemos copia de la lista completa a contactosListaOriginal para recuperar el
        //estado despues de la busquedas
        contactosListaOriginal = ArrayList(contactosLista)


        // Adaptador para RecyclerView:
        contactosAdaptador = AdapterContacto(contactosLista,
            //Add dos metodos que utilizan las funciones lambdas para la comunicacion entre clases:
            //Pasamos un contacto, y nos hace la siguiente operacion:
            favClick = { contacto ->
                val contactoId = contacto.id
                if (contactoId != null) {
                    //Cambiar el valor actual por el contrario:
                    val nuevoVip = contacto.vip != true
                    //
                    database.child("contactos").child(contactoId).child("vip").setValue(nuevoVip)
                        .addOnCompleteListener {
                            //Actualizamos el estado en la lista local para que actualice correctamente:
                            contacto.vip = nuevoVip
                            //Notificar al adaptador solo para este ítem
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
                val intent = Intent(this, EditarContactoActivity::class.java)
                intent.putExtra("contactoId", contacto.id) // Pasar solo el ID del contacto
                startActivityForResult(intent, REQUEST_CODE_EDITAR_CONTACTO)
            })

        contactosRecycler.adapter = contactosAdaptador


        //Metodos para hacer el swipe, desplazamientos laterales, para los contactos, utilizamos itemTouchHelper para
        //identificar si se desplaza el elemenmto en el recyclerView y responder acorde a ello:
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

            //Este primer metodo se encarga de que los elementos no acaben perdiendose o eliminandose de la lista por desplazarlos:
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false // No mover los elementos
            }

            //Este segundo metodo vera la direccion donde estoy desplazando el elemento y hara una accion y otra:
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val contacto = contactosLista[position]

                //En este caso le decimos que si deplaza hacia la izquierda, entraremos en una actividad para enviar un mensaje:
                if (direction == ItemTouchHelper.LEFT) {
                    // Deslizar hacia la izquierda para enviar un mensaje
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${contacto.numeroUno}"))
                    intent.putExtra("sms_body", "¡Hola! ¿Cómo estás?")
                    startActivity(intent)

                //En este caso le decimos que si deplaza hacia la derecha, entraremos en una actividad para hacer una llamada:
                } else if (direction == ItemTouchHelper.RIGHT) {
                    // Deslizar hacia la derecha para realizar una llamada
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contacto.numeroUno}"))
                    startActivity(intent)
                }

                //Hay que asegurar que se muestran los datos:
                contactosAdaptador.notifyItemChanged(position)
            }

            //Este metodo es donde reside toda la animacion y colores que se ven cuando se desplaza el elemento, utilizamos
            //componentes como Canvas para hacer dibujas de colores, etc:
            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                                     dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                //Tamaño de los iconos:
                val iconWidth = 200f // El tamaño del ícono que dibujaremos
                val backgroundPaint = Paint()

                //Si se deslizamos hacia la izquierda, enviaremos un mensaje y dibujamos icono y colores de mensaje:
                if (dX < 0) {
                    //Color azul para mensajes
                    backgroundPaint.color = Color.BLUE

                    //Dibujamos el fondo azul
                    c.drawRect(viewHolder.itemView.right.toFloat() + dX, viewHolder.itemView.top.toFloat(),
                        viewHolder.itemView.right.toFloat(), viewHolder.itemView.bottom.toFloat(), backgroundPaint)

                    //Dibujamos el icono de mensajes, lo convierte y lo dibuja:
                    val icon = ContextCompat.getDrawable(applicationContext, R.drawable.ic_message)
                    icon?.setBounds(
                        viewHolder.itemView.right - iconWidth.toInt(),
                        viewHolder.itemView.top + (viewHolder.itemView.height / 2 - iconWidth.toInt() / 2),
                        viewHolder.itemView.right,
                        viewHolder.itemView.top + (viewHolder.itemView.height / 2 + iconWidth.toInt() / 2)
                    )
                    icon?.draw(c)

                }
                // Si se desliza hacia la derecha, mismo procedimiento pero al contrario para la llamada:
                else if (dX > 0) {

                    //Color verde para llamada
                    backgroundPaint.color = Color.GREEN

                    //Dibujamos el fondo verde
                    c.drawRect(viewHolder.itemView.left.toFloat(), viewHolder.itemView.top.toFloat(),
                        viewHolder.itemView.left.toFloat() + dX, viewHolder.itemView.bottom.toFloat(), backgroundPaint)

                    //Y por ultimo el icono:
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
        //Importante una vez definido el TouchHelper es asociarlo:
        itemTouchHelper.attachToRecyclerView(contactosRecycler)


        //Una vez terminado primeros enlaces y cargas, pasamos a cargar los datos de Firebase
        cargarDatos()

        //Preparamos la barra de busqueda para los contactos:
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            //Cuando el texto cambia, ejecutamos esto:
            override fun onQueryTextChange(newText: String?): Boolean {
                //Aqui es donde estamos filtrando los contactos a la vez que vamos escribiendo en la barra de busqueda
                filterContactos(newText)
                return true
            }
        })


        //Add la funcionalidad para el boton Buscar
        btnBuscar.setOnClickListener {
            if (searchView.visibility == View.GONE) {
                //Mostrar la barra de búsqueda
                searchView.visibility = View.VISIBLE
            } else {
                //Ocultar la barra de búsqueda
                searchView.visibility = View.GONE

                //Limpiamos despues de cerrar la búsqueda
                searchView.setQuery("", false)
                //Y volvemos a mostrar
                filterContactos("")
            }
        }

        //Add la funcionalidad para el boton para agregar un contacto
        binding.btnAniadir.setOnClickListener {
            //Ejecuta la actvidad para add un contacto
            val intent = Intent(this, AgregarContactoActivity::class.java)
            startActivity(intent)
        }
    }



    //Metodo para filtrar la lista con el buscador:
    private fun filterContactos(query: String?) {
        val filteredList = if (query.isNullOrEmpty()) {
            //Si no hay texto, restauramos la lista completa desde contactosListaOriginal
            contactosListaOriginal
        } else {
            //Filtramos los contactos por el nombre
            contactosListaOriginal.filter { it.nombre!!.contains(query, ignoreCase = true) }
        }

        //Ordenamos la lista filtrada
        val sortedList = filteredList.sortedWith(compareByDescending<Contacto> { it.vip }.thenBy { it.nombre })

        //Actualizamos el adaptador con la nueva lista ordenada
        contactosAdaptador.updateList(sortedList)
    }


    //Metodo para recuperar los datos de Firebase
    private fun cargarDatos() {
        //Cuando cargamos los datos, limpiamos las listas locales, tanto principal como auxiliar:
        contactosLista.clear()
        contactosListaOriginal.clear()


        //Iniciamos la sobreescritura de los metodos necesarios para realizar observaciones de
        //cambios, y actuar especificamente sobre los nodos hijos afectados, evitando un uso
        //constante de la lista completa:
        database.child("contactos").addChildEventListener(object : ChildEventListener {

            //Cuando se agrega un contacto:
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val contacto = snapshot.getValue(Contacto::class.java)

                if (contacto != null && contactosLista.none { it.id == contacto.id }) {
                    contactosLista.add(contacto)
                    //Actualizamos la lista original después de cargar los datos
                    contactosListaOriginal = ArrayList(contactosLista)
                    //Y ordenamos los contactos antes de mostrar en el recyclerView:
                    ordenarContactos()
                    contactosAdaptador.notifyItemInserted(contactosLista.size - 1)
                }
            }

            //Cuando ese contacto cambia en el firebase:
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val contacto = snapshot.getValue(Contacto::class.java)
                if (contacto != null) {
                    val index = contactosLista.indexOfFirst { it.id == contacto.id }
                    if (index != -1) {
                        contactosLista[index] = contacto
                        ordenarContactos()
                        contactosListaOriginal = ArrayList(contactosLista)
                        contactosAdaptador.notifyItemChanged(index)
                    }
                }
            }

            //Cuando ese contacto se elimina en el firebase:
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val contacto = snapshot.getValue(Contacto::class.java)
                if (contacto != null) {
                    val index = contactosLista.indexOfFirst { it.id == contacto.id }
                    if (index != -1) {
                        contactosLista.removeAt(index)
                        contactosAdaptador.notifyItemRemoved(index)
                        contactosListaOriginal = ArrayList(contactosLista)
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


    //Ordenador de los contactos:
    private fun ordenarContactos() {
        //Ordena los contactos por favoritos primero, y luego por nombre:
        contactosLista.sortWith(compareByDescending<Contacto> { it.vip }.thenBy { it.nombre })
        contactosAdaptador.notifyDataSetChanged()
    }

    //Este metodo lo utilizo para tener control sobre los datos borrados, cuando se vuelve de la actividad
    //de edicion, si el contacto a sido borrado y era el ultimo, se quedaba en la lista, con estos metodos
    //se consigue que el programa se de cuenta de los cambios y registra bien la lista:
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_EDITAR_CONTACTO && resultCode == Activity.RESULT_OK) {
            val eliminado = data?.getBooleanExtra("contactoEliminado", false) ?: false
            if (eliminado) {
                cargarDatos() // Recargar la lista desde Firebase
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_EDITAR_CONTACTO = 1001
    }
}
