package com.swipesquad.memory

import org.json.JSONArray

fun List<List<QRItem>>.toJsonArray(): JSONArray {
    val outerArray = JSONArray()
    this.forEach { pair ->
        val innerArray = JSONArray()
        pair.forEach { item ->
            innerArray.put(item.value)
        }
        outerArray.put(innerArray)
    }
    return outerArray
}