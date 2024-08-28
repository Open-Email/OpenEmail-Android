package com.mercata.pingworks.inbox_list

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.common.ListState
import com.mercata.pingworks.models.BroadcastMessage
import com.mercata.pingworks.models.Person
import java.time.ZonedDateTime

class InboxListViewModel :
    AbstractViewModel<BroadcastListState>(BroadcastListState(messages = mutableStateListOf())) {

    init {
        //TODO remove test data
        for (i in 0..50) {
            currentState.messages.add(
                BroadcastMessage(
                    id = i.toString(),
                    subject = "Some message subject might be longer then expected so here we are",
                    person = Person(
                        name = "Pallas's cat",
                        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d6/Manoel.jpg/440px-Manoel.jpg"
                    ),
                    date = ZonedDateTime.now(),
                    body = "The Pallas's cat (Otocolobus manul), also known as the manul, is a small wild cat with long and dense light grey fur, and rounded ears set low on the sides of the head. Its head-and-body length ranges from 46 to 65 cm (18 to 26 in) with a 21 to 31 cm (8.3 to 12.2 in) long bushy tail. It is well camouflaged and adapted to the cold continental climate in its native range, which receives little rainfall and experiences a wide range of temperatures.\n" +
                            "\n" +
                            "The Pallas's cat was first described in 1776 by Peter Simon Pallas, who observed it in the vicinity of Lake Baikal. Since then, it has been recorded across a large region in Central Asia, albeit in widely spaced sites from the Caucasus, Iranian Plateau, Hindu Kush, parts of the Himalayas, Tibetan Plateau to the Altai-Sayan region and South Siberian Mountains. It inhabits rocky montane grasslands and shrublands, where the snow cover is below 15–20 cm (6–8 in). It finds shelter in rock crevices and burrows, and preys foremost on lagomorphs and rodents. The female gives birth to between two and six kittens in spring.\n" +
                            "\n" +
                            "Due to its widespread range and assumed large population, the Pallas's cat has been listed as Least Concern on the IUCN Red List since 2020. Some population units are threatened by poaching, prey base decline due to rodent control programs, and habitat fragmentation as a result of mining and infrastructure projects.\n" +
                            "\n" +
                            "The Pallas's cat has been kept in zoos since the early 1950s. As of 2018, 60 zoos in Europe, Russia, North America and Japan participate in Pallas's cat captive breeding programs."
                )
            )
        }

        updateState(currentState)
    }

    fun removeItem(item: BroadcastMessage) {
        currentState.messages.remove(item)
        updateState(currentState)
    }
}

data class BroadcastListState(override val messages: SnapshotStateList<BroadcastMessage>): ListState