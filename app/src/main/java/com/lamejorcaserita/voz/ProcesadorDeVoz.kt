package com.lamejorcaserita.voz

import com.lamejorcaserita.data.entity.Producto
import java.text.Normalizer

/**
 * Procesa el texto reconocido por voz y lo transforma en una lista de
 * productos con cantidades, listos para agregar a un pedido.
 *
 * Algoritmo:
 * 1. Normalizar el texto.
 * 2. Dividir el texto en fragmentos separados por "y", ",", "más".
 * 3. Para cada fragmento, encontrar el mejor producto candidato.
 * 4. En caso de empate, priorizar el que tenga más palabras clave coincidentes.
 * 5. Detectar la cantidad asociada.
 */
object ProcesadorDeVoz {

    data class Resultado(
        val itemsDetectados: List<ItemDetectado>,
        val productosNoEncontrados: List<String>
    )

    data class ItemDetectado(
        val producto: Producto,
        val cantidad: Int
    )

    private val cantidadesEscritas = mapOf(
        "un" to 1, "una" to 1, "uno" to 1,
        "dos" to 2,
        "tres" to 3,
        "cuatro" to 4,
        "cinco" to 5,
        "seis" to 6,
        "siete" to 7,
        "ocho" to 8,
        "nueve" to 9,
        "diez" to 10,
        "once" to 11,
        "doce" to 12,
        "quince" to 15,
        "veinte" to 20
    )

    private val palabrasIgnoradas = setOf(
        "de", "del", "la", "el", "los", "las", "un", "una", "unos", "unas",
        "y", "con", "sin", "para", "por", "en", "a", "mas", "menos"
    )

    fun procesar(texto: String, productosDisponibles: List<Producto>): Resultado {
        val textoNormalizado = normalizar(texto)
        if (textoNormalizado.isBlank()) return Resultado(emptyList(), emptyList())

        // Dividir en fragmentos por "y", ",", "más"
        val fragmentos = dividirEnFragmentos(textoNormalizado)

        val itemsDetectados = mutableListOf<ItemDetectado>()
        val idsYaDetectados = mutableSetOf<Int>()

        // Procesar cada fragmento por separado
        for (fragmento in fragmentos) {
            val palabrasFragmento = fragmento.split("\\s+".toRegex()).filter { it.isNotBlank() }
            if (palabrasFragmento.isEmpty()) continue

            val mejorCandidato = encontrarMejorProducto(palabrasFragmento, productosDisponibles)
            if (mejorCandidato != null) {
                val cantidad = detectarCantidad(palabrasFragmento, mejorCandidato.posicion)
                itemsDetectados.add(ItemDetectado(mejorCandidato.producto, cantidad))
                idsYaDetectados.add(mejorCandidato.producto.id)
            }
        }

        // Si no se detectó nada en fragmentos, intentar con el texto completo
        if (itemsDetectados.isEmpty()) {
            val palabrasCompletas = textoNormalizado.split("\\s+".toRegex()).filter { it.isNotBlank() }
            val mejorCandidato = encontrarMejorProducto(palabrasCompletas, productosDisponibles)
            if (mejorCandidato != null) {
                val cantidad = detectarCantidad(palabrasCompletas, mejorCandidato.posicion)
                itemsDetectados.add(ItemDetectado(mejorCandidato.producto, cantidad))
            }
        }

        return Resultado(itemsDetectados, emptyList())
    }

    private data class Candidato(
        val producto: Producto,
        val score: Double,
        val coincidencias: Int,
        val posicion: Int
    )

    /**
     * Encuentra el mejor producto candidato para un conjunto de palabras.
     * Prioriza por: (1) mayor score, (2) mayor número de coincidencias.
     */
    private fun encontrarMejorProducto(
        palabras: List<String>,
        productosDisponibles: List<Producto>
    ): Candidato? {
        val candidatos = mutableListOf<Candidato>()

        for (producto in productosDisponibles) {
            val palabrasProducto = normalizar(producto.nombre)
                .split("\\s+".toRegex())
                .filter { it.isNotBlank() && it !in palabrasIgnoradas }

            if (palabrasProducto.isEmpty()) continue

            var coincidencias = 0
            var posicionPrimera = -1

            palabrasProducto.forEach { palabraProducto ->
                val stemProducto = aplicarStem(palabraProducto)
                palabras.forEachIndexed { index, palabraTexto ->
                    if (aplicarStem(palabraTexto) == stemProducto) {
                        coincidencias++
                        if (posicionPrimera == -1 || index < posicionPrimera) {
                            posicionPrimera = index
                        }
                    }
                }
            }

            if (coincidencias == 0) continue

            val score = coincidencias.toDouble() / palabrasProducto.size
            candidatos.add(Candidato(producto, score, coincidencias, posicionPrimera))
        }

        if (candidatos.isEmpty()) return null

        // Ordenar por: score descendente, luego coincidencias descendentes
        val ordenados = candidatos.sortedWith(
            compareByDescending<Candidato> { it.score }
                .thenByDescending { it.coincidencias }
        )

        val mejor = ordenados.first()
        val segundo = ordenados.getOrNull(1)

        // Si el mejor tiene score perfecto (1.0), devolverlo directo
        if (mejor.score >= 1.0) return mejor

        // Si hay empate en score con el segundo, desempatar por coincidencias
        if (segundo != null && segundo.score == mejor.score) {
            return if (mejor.coincidencias >= segundo.coincidencias) mejor else segundo
        }

        return mejor
    }

    /**
     * Divide el texto en fragmentos por separadores comunes.
     */
    private fun dividirEnFragmentos(texto: String): List<String> {
        // Separadores: "y", ",", "más"
        return texto
            .split("\\s+(y|,|mas)\\s+".toRegex())
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun normalizar(texto: String): String {
        val sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
        return sinTildes
            .lowercase()
            .replace("[^a-z0-9\\s]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private fun aplicarStem(palabra: String): String {
        return if (palabra.length > 3 && palabra.endsWith("s")) {
            palabra.dropLast(1)
        } else {
            palabra
        }
    }

    private fun detectarCantidad(palabras: List<String>, indiceProducto: Int): Int {
        val desdeIndice = maxOf(0, indiceProducto - 3)
        for (i in indiceProducto - 1 downTo desdeIndice) {
            val palabra = palabras.getOrNull(i) ?: continue

            palabra.toIntOrNull()?.let { return it }

            val palabraStem = aplicarStem(palabra)
            cantidadesEscritas[palabra]?.let { return it }
            cantidadesEscritas[palabraStem]?.let { return it }
        }
        return 1
    }
}