import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileReader
import java.io.InputStreamReader

main()

fun main() {
    println("#id,nom EN,nom FR,type 1,type 2,vulnérabilités,résistances,dangereux pour,inoffensif pour\n")
    val path = "C:\\Users\\samuel_lemoine\\Downloads\\Nouveau dossier\\pokemon\\quickédex\\resources\\"
    val pokemon_types = path + "pokemon_types.csv"
    val type_efficacy = path + "type_efficacy.csv"
    val i18n = path + "pokemon_species_names.csv"

    val pokemons = mutableMapOf<Int, Pokemon>()
    
    var first = true

    /* pokemons types */
    BufferedReader(FileReader(pokemon_types)).use { reader ->
        // pokemon_id,type_id,slot
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            if(first) {first = false; continue }
            val split = line?.split(',') ?: continue
            val id = split[0].toInt()
            if(id > 10000) continue
            val type = split[1].toInt()
            val slot = split[2].toInt()
            if (slot == 1) {
                val pokemon = Pokemon(
                    id = id,
                    en = "",
                    fr = "",
                    type1 = type,
                    type2 = 0,
                    vulnerabilities = mutableListOf(),
                    resistances = mutableListOf(),
                    dangerousFor = mutableListOf(),
                    weakAgainst = mutableSetOf(),
                    ratio = 1.0,
                )
                // Ajouter le Pokémon à la Map des pokemons
                pokemons[id] = pokemon
            } else {
                pokemons[id]?.type2 = type
            }
        }
    }

    /* pokemons i18n */
    BufferedReader(InputStreamReader(FileInputStream(i18n), "UTF-8")).use { reader ->
        // pokemon_species_id,local_language_id,name,genus
        // 5 = FR, 7 = EN
        var line: String?
        first = true
        while (reader.readLine().also { line = it } != null) {
            if(first) {first = false; continue }
            val split = line?.split(',') ?: continue
            val id = split[0].toInt()
            val locale = split[1].toInt()
            val name = split[2]
            if(locale == 5) {
                pokemons[id]?.fr = name
            } else if(locale == 7) {
                pokemons[id]?.en = name
            } else continue
        }
    }

    /* types interactions */
    val ratioByDamage = Array(18) { Array(18) { 0.0 } }
    val ratioByTarget = Array(18) { Array(18) { 0.0 } }
    BufferedReader(FileReader(type_efficacy)).use { reader ->
        // damage_type_id,target_type_id,damage_factor
        var line: String?
        first = true
        while (reader.readLine().also { line = it } != null) {
            if(first) {first = false; continue }
            val split = line?.split(',') ?: continue
            val damage = split[0].toInt() -1
            val target = split[1].toInt() -1
            val factor = split[2].toDouble() / 100
            ratioByDamage[damage][target] = factor
            ratioByTarget[target][damage] = factor
        }
    }
    pokemons.values.forEach {pk ->
        ratioByDamage[pk.type1 -1].forEachIndexed { i, impact ->
            val targetType = i + 1
            if(impact > 1) {
                pk.dangerousFor.add(targetType)
            } else if (impact < 1) {
                pk.weakAgainst.add(targetType)
            }
        }
        if(pk.type2 >0) {
            ratioByDamage[pk.type2 - 1].forEachIndexed { i, impact ->
                val targetType = i + 1
                if (impact > 1) {
                    pk.dangerousFor.add(targetType)
                    pk.weakAgainst.remove(targetType)
                } else if (impact < 1) {
                    if(!pk.dangerousFor.contains(targetType)) {
                        pk.weakAgainst.add(targetType)
                    }
                }
            }
        }

        val pkSensibilities = ratioByTarget[pk.type1 - 1].copyOf()
        if(pk.type2 >0) {
            ratioByTarget[pk.type2 - 1].forEachIndexed { i, impact ->
                pkSensibilities[i] = pkSensibilities[i] * impact
            }
        }
        pkSensibilities.forEachIndexed { i, impact ->
            val damageType = i + 1
            if(impact > 1) {
                pk.vulnerabilities.add(damageType)
            } else if (impact < 1) {
                pk.resistances.add(damageType)
            }
        }

        pk.ratio = pk.resistances.size.toDouble() / pk.vulnerabilities.size
    }

    printHTML(pokemons)
}

data class Pokemon(
    val id: Int,
    var en: String,
    var fr: String,
    var type1: Int,
    var type2: Int,
    val vulnerabilities: MutableList<Int>,
    val resistances: MutableList<Int>,
    val dangerousFor: MutableList<Int>,
    val weakAgainst: MutableSet<Int>,
    var ratio: Double,
)

fun printTypes(types: Collection<Int>) {
    var first = true
    types.forEach {
        if (first) {
            first = false
        } else {
            print(' ')
        }
        print(it)
    }
}
fun printTypesTD(types: Collection<Int>) {
    print("<td>")
    types.forEach {
        print("<span class=\"type-icon type-%d\"></span>".format(it))
    }
    print("</td>")
}
fun printTD(content: Any) {
    print("<td>")
    print(content)
    print("</td>")
}
fun printTD(type: Int, type2: Int) {
    print("<td>")
    printSpan(type)
    if(type2 > 0) {
        printSpan(type2)
    }
    print("</td>")
}
fun printSpan(type: Int) {
    print("<span class=\"type-icon type-%d\"></span>".format(type))
}

fun printCSV(pokemons: MutableMap<Int, Pokemon>) {
    pokemons.values.forEach {
        print(it.id)
        print(',')
        print(it.en)
        print(',')
        print(it.fr)
        print(',')
        print(it.type1)
        print(',')
        print(it.type2)
        print(',')
        printTypes(it.vulnerabilities)
        print(',')
        printTypes(it.resistances)
        print(',')
        printTypes(it.dangerousFor)
        print(',')
        printTypes(it.weakAgainst)
        println()
    }
}
fun printHTML(pokemons: MutableMap<Int, Pokemon>) {
    pokemons.values.forEach {
        print("<tr>")
        printTD(it.id)
        printTD(it.en)
        printTD(it.fr)
        printTD(it.type1, it.type2)
        printTypesTD(it.vulnerabilities)
        printTypesTD(it.resistances)
        printTypesTD(it.dangerousFor)
        printTypesTD(it.weakAgainst)
        printTD("%.2f".format(it.ratio))
        print("</tr>")
        println()
    }
}