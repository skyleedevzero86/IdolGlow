package com.sleekydz86.idolglow.webzine.application

interface WebzineImageStoragePort {
    fun store(command: StoreWebzineImageCommand): StoredWebzineImage
}
