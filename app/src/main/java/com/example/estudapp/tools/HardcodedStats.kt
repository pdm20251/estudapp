package com.example.estudapp.tools

import android.util.Log
import com.example.estudapp.data.model.FlashcardDTO
import com.example.estudapp.domain.repository.FlashcardRepository
import com.example.estudapp.domain.stats.DeckSessionManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlin.math.round
import kotlin.random.Random

object HardcodedStats {

    private const val TAG = "HardcodedStats"

    /** Grava sessão DEMO com IDs “fake” + lat/lng aleatórios. */
    suspend fun writeSampleSession(deckId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("Usuário não autenticado.")

        val session = DeckSessionManager(deckId, uid)

        // NOVO: localização aleatória (6 casas decimais)
        val (lat, lng) = randomLatLng()
        session.setLocation(lat, lng)

        // Frente/Verso → 0/0
        session.addFrenteVerso(cardId = "cardFV_demo")
        // Múltipla → correta
        session.addMultiplaEscolha(cardId = "cardMC_demo", isCorrect = true)
        // Cloze → nota fracionada
        session.addCloze(cardId = "cardCZ_demo", aiScore = 8.5)
        // Digite → nota fracionada
        session.addDigiteResposta(cardId = "cardDR_demo", aiScore = 7.25)

        val dto = session.build()
        val repo = FlashcardRepository()
        val sessionId = repo.saveDeckSessionStat(dto).getOrElse { throw it }
        Log.i(TAG, "Sessão DEMO salva em stats/$uid/$deckId/$sessionId (lat=$lat, lng=$lng)")
    }

    /** Grava sessão usando os cards reais do deck + lat/lng aleatórios. */
    suspend fun writeSessionFromExistingCards(deckId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("Usuário não autenticado.")

        val repo = FlashcardRepository()
        val cardsRes = repo.getFlashcards(deckId).first()
        val cards = cardsRes.getOrElse { throw it }
        if (cards.isEmpty()) throw IllegalStateException("Deck sem flashcards.")

        val session = DeckSessionManager(deckId, uid)

        // NOVO: localização aleatória
        val (lat, lng) = randomLatLng()
        session.setLocation(lat, lng)

        cards.forEachIndexed { idx, c -> simulateCard(session, c, idx) }

        val sessionId = repo.saveDeckSessionStat(session.build()).getOrElse { throw it }
        Log.i(TAG, "Sessão REAL salva em stats/$uid/$deckId/$sessionId (lat=$lat, lng=$lng)")
    }

    private fun simulateCard(session: DeckSessionManager, card: FlashcardDTO, idx: Int) {
        val id = card.id ?: return
        when (card.type) {
            "FRENTE_VERSO"     -> session.addFrenteVerso(id)
            "MULTIPLA_ESCOLHA" -> session.addMultiplaEscolha(id, isCorrect = idx % 2 == 0)
            "CLOZE"            -> session.addCloze(id, aiScore = 8.0)
            "DIGITE_RESPOSTA"  -> session.addDigiteResposta(id, aiScore = 7.25)
            else               -> session.addFrenteVerso(id)
        }
    }

    /** Lat -90..90, Lng -180..180, arredondados a 6 casas. */
    private fun randomLatLng(): Pair<Double, Double> {
        val lat = Random.nextDouble(-90.0, 90.0)
        val lng = Random.nextDouble(-180.0, 180.0)
        return Pair(round6(lat), round6(lng))
    }

    private fun round6(v: Double) = round(v * 1_000_000) / 1_000_000.0
}