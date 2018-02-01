package net.airvantage.model

class User {

    var uid: String? = null
    var email: String? = null
    var name: String? = null
    var profile: Profile? = null
    var company: Company? = null
    var server: String? = null

    inner class Profile {
        var uid: String? = null
        var name: String? = null
    }

    inner class Company {
        var uid: String? = null
        var name: String? = null
    }


}
