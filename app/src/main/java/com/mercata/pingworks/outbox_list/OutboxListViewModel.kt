package com.mercata.pingworks.outbox_list

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.common.ListState
import com.mercata.pingworks.models.BroadcastMessage
import com.mercata.pingworks.models.Person
import java.time.ZonedDateTime

class OutboxListViewModel :
    AbstractViewModel<BroadcastListState>(BroadcastListState(messages = mutableStateListOf())) {

    init {
        //TODO remove test data
        for (i in 0..500) {
            currentState.messages.add(
                BroadcastMessage(
                    id = i.toString(),
                    subject = "Some message subject might be longer then expected so here we are",
                    person = Person(
                        name = "Red panda",
                        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e6/Red_Panda_%2824986761703%29.jpg/440px-Red_Panda_%2824986761703%29.jpg"
                    ),
                    date = ZonedDateTime.now(),
                    body = "The red panda (Ailurus fulgens), also known as the lesser panda, is a small mammal native to the eastern Himalayas and southwestern China. It has dense reddish-brown fur with a black belly and legs, white-lined ears, a mostly white muzzle and a ringed tail. Its head-to-body length is 51–63.5 cm (20.1–25.0 in) with a 28–48.5 cm (11.0–19.1 in) tail, and it weighs between 3.2 and 15 kg (7.1 and 33.1 lb). It is well adapted to climbing due to its flexible joints and curved semi-retractile claws.\n" +
                            "\n" +
                            "The red panda was formally described in 1825. The two currently recognised subspecies, the Himalayan and the Chinese red panda, genetically diverged about 250,000 years ago. The red panda's place on the evolutionary tree has been debated, but modern genetic evidence places it in close affinity with raccoons, weasels, and skunks. It is not closely related to the giant panda, which is a bear, though both possess elongated wrist bones or \"false thumbs\" used for grasping bamboo. The evolutionary lineage of the red panda (Ailuridae) stretches back around 25 to 18 million years ago, as indicated by extinct fossil relatives found in Eurasia and North America.\n" +
                            "\n" +
                            "The red panda inhabits coniferous forests as well as temperate broadleaf and mixed forests, favouring steep slopes with dense bamboo cover close to water sources. It is solitary and largely arboreal. It feeds mainly on bamboo shoots and leaves, but also on fruits and blossoms. Red pandas mate in early spring, with the females giving birth to litters of up to four cubs in summer. It is threatened by poaching as well as destruction and fragmentation of habitat due to deforestation. The species has been listed as Endangered on the IUCN Red List since 2015. It is protected in all range countries.\n" +
                            "\n" +
                            "Community-based conservation programmes have been initiated in Nepal, Bhutan and northeastern India; in China, it benefits from nature conservation projects. Regional captive breeding programmes for the red panda have been established in zoos around the world. It is featured in animated movies, video games, comic books and as the namesake of companies and music bands."
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