/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:kotlin-analysis-api/src/main/kotlin/com/google/devtools/ksp/common/PersistentMap.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: パッケージ名のみ変更
package me.tbsten.analysis.code.gen.ksp.bridge.common

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.BufferedInputStream
import java.io.File

private object FileSerializer : KSerializer<File> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("File", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: File) = encoder.encodeString(value.path)
    override fun deserialize(decoder: Decoder): File = File(decoder.decodeString())
}

private object SymbolSerializer : KSerializer<LookupSymbolWrapper> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LookupSymbolWrapper", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LookupSymbolWrapper) =
        encoder.encodeString("${value.name}:${value.scope}")
    override fun deserialize(decoder: Decoder): LookupSymbolWrapper {
        val (name, scope) = decoder.decodeString().split(':')
        return LookupSymbolWrapper(name, scope)
    }
}

private val fileToFilesMapSerializer = MapSerializer(FileSerializer, ListSerializer(FileSerializer))
private val fileToSymbolsMapSerializer = MapSerializer(FileSerializer, ListSerializer(SymbolSerializer))

abstract class PersistentMap<K, V>(
    private val serializer: KSerializer<Map<K, V>>,
    private val storage: File,
    private val m: MutableMap<K, V>,
) : MutableMap<K, V> by m {

    @OptIn(ExperimentalSerializationApi::class)
    fun flush() {
        storage.outputStream().use {
            Json.encodeToStream(serializer, m.toMap(), it)
        }
    }
    override fun toString() = m.toString()

    companion object {
        @JvmStatic
        @OptIn(ExperimentalSerializationApi::class)
        protected fun <K, V> deserialize(serializer: KSerializer<Map<K, V>>, storage: File): MutableMap<K, V> {
            return if (storage.exists()) {
                BufferedInputStream(storage.inputStream()).use {
                    Json.decodeFromStream(serializer, it).toMutableMap()
                }
            } else {
                mutableMapOf()
            }
        }
    }
}

class FileToFilesMap(
    storage: File
) : PersistentMap<File, List<File>>(fileToFilesMapSerializer, storage, deserialize(fileToFilesMapSerializer, storage))

class FileToSymbolsMap(
    storage: File
) : PersistentMap<File, List<LookupSymbolWrapper>>(
    fileToSymbolsMapSerializer,
    storage,
    deserialize(fileToSymbolsMapSerializer, storage)
)
