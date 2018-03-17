package dev.yn.playground.common.models

sealed class Reference<ID, out Domain> {
    abstract val id: ID
    abstract val referenced: Domain?

    data class Defined<ID, Domain>(override val id: ID, override val referenced: Domain): Reference<ID, Domain>()
    data class Empty<ID, out Domain>(override val id: ID): Reference<ID, Domain>() {
        override val referenced: Domain? = null
    }
}
