package net.nemerosa.ontrack.boot.support

import com.fasterxml.jackson.annotation.JsonIgnore
import net.nemerosa.ontrack.model.structure.NameDescription
import java.util.*

class APIInfo(
        @JsonIgnore
        val type: String,
        nd: NameDescription) {

    val name: String = nd.name
    val description: String = nd.description

    val methods = ArrayList<APIMethodInfo>()


    fun add(methodInfo: APIMethodInfo): APIInfo {
        methods.add(methodInfo)
        return this
    }

}
