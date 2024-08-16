package com.mercata.pingworks.broadcast_list

import com.mercata.pingworks.AbstractViewModel
import com.mercata.pingworks.models.BroadcastMessage
import com.mercata.pingworks.models.Person
import java.time.LocalDateTime

class BroadcastListViewModel :
    AbstractViewModel<BroadcastListState>(BroadcastListState(messages = mutableListOf())) {

    init {
        //TODO remove test data
        for (i in 0..20) {
            currentState.messages.add(
                BroadcastMessage(
                    id = i.toString(),
                    subject = "Some message subject might be longer then expected so here we are",
                    person = Person(
                        name = "Joanno Dopelhetz",
                        imageUrl = "https://i.pinimg.com/736x/e9/59/8a/e9598aec9c6f3ab0afd9d604c0b68960.jpg"
                    ),
                    date = LocalDateTime.now(),
                    body = "The giant panda (Ailuropoda melanoleuca), also known as the panda bear or simply panda, is a bear species endemic to China. It is characterised by its white coat with black patches around the eyes, ears, legs and shoulders. Its body is rotund; adult individuals weigh 100 to 115 kg (220 to 254 lb) and are typically 1.2 to 1.9 m (3 ft 11 in to 6 ft 3 in) long. It is sexually dimorphic, with males being typically 10 to 20% larger than females. A thumb is visible on its forepaw, which helps in holding bamboo in place for feeding. It has large molar teeth and expanded temporal fossa to meet its dietary requirements. It can digest starch and is mostly herbivorous with a diet consisting almost entirely of bamboo and bamboo shoots.\n" +
                            "\n" +
                            "The giant panda lives exclusively in six montane regions in a few Chinese provinces at elevations of up to 3,000 m (9,800 ft). It is solitary and gathers only in mating seasons. It relies on olfactory communication to communicate and uses scent marks as chemical cues and on landmarks like rocks or trees. Females rear cubs for an average of 18 to 24 months. The oldest known giant panda was 38 years old.\n" +
                            "\n" +
                            "As a result of farming, deforestation and infrastructural development, the giant panda has been driven out of the lowland areas where it once lived. The wild population has increased again to 1,864 individuals as of March 2015. Since 2016, it has been listed as Vulnerable on the IUCN Red List. In July 2021, Chinese authorities also classified the giant panda as vulnerable. It is a conservation-reliant species. By 2007, the captive population comprised 239 giant pandas in China and another 27 outside the country. It has often served as China's national symbol, appeared on Chinese Gold Panda coins since 1982 and as one of the five Fuwa mascots of the 2008 Summer Olympics held in Beijing."
                )
            )
        }

        updateState(currentState)
    }
}

data class BroadcastListState(val messages: MutableList<BroadcastMessage>)