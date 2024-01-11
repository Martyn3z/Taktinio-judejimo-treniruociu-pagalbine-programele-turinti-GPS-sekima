import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

// Funkcija, skirta generuoti SHA-256 maišos funkciją
fun sha256(input: String): String? {
    return try {
        // Konvertuojame įvesties eilutę į baitų masyvą
        val bytes = input.toByteArray()
        // Sukuriame MessageDigest egzempliorių SHA-256 algoritmui
        val md = MessageDigest.getInstance("SHA-256")
        // Apskaičiuojame maišos funkciją
        val digest = md.digest(bytes)
        // Konvertuojame maišos funkcijos baitų masyvą į šešioliktainę eilutę
        digest.fold("") { str, it -> str + "%02x".format(it) }
    } catch (e: NoSuchAlgorithmException) {
        // Jeigu įvyko klaida, grąžiname null
        null
    }
}
