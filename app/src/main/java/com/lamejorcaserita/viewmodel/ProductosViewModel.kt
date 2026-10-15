package com.lamejorcaserita.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lamejorcaserita.data.AppDatabase
import com.lamejorcaserita.data.entity.Producto
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProductosViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.obtenerBaseDeDatos(application)
    private val productoDao = db.productoDao()

    val productos: StateFlow<List<Producto>> = productoDao.obtenerTodos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun agregarProducto(nombre: String, precio: Int) {
        viewModelScope.launch {
            productoDao.insertar(Producto(nombre = nombre, precio = precio))
        }
    }

    fun actualizarProducto(producto: Producto) {
        viewModelScope.launch {
            productoDao.actualizar(producto)
        }
    }

    fun eliminarProducto(producto: Producto) {
        viewModelScope.launch {
            productoDao.eliminar(producto)
        }
    }
}