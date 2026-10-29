// Feature tour: shown once before the profile setup on first launch, and on demand from Plus → Développeur.
package com.clement.nutrimo.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.ui.common.WideButton
import com.clement.nutrimo.ui.common.splashGone
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// How long each page stays before moving on: the welcome bloom is short, the feature pages have more to read.
private const val WELCOME_DWELL_MS = 4000L
private const val PAGE_DWELL_MS = 6000L

/** One page: its text and its scene, which plays each time the page becomes the current one. */
private class IntroPage(val title: String, val text: String, val art: @Composable (active: Boolean) -> Unit)

private val PAGES = listOf(
    IntroPage(
        "Bienvenue sur Nutrimo",
        "Perdez du poids à votre rythme, une bonne habitude à la fois. Toutes vos données restent sur votre téléphone.",
    ) { BloomArt(it) },
    IntroPage(
        "Comptez vos calories",
        "Cherchez un aliment, scannez un code-barres ou enregistrez vos recettes. L'anneau du journal montre où vous en êtes : " +
            "vert dans l'objectif, jaune encore sous votre maintenance, rouge au-delà.",
    ) { CaloriesArt(it) },
    IntroPage(
        "Suivez votre corps",
        "Pesées, courbe de tendance et date d'arrivée estimée, IMC et mensurations : voyez aussi les progrès que la balance ne montre pas.",
    ) { WeightArt(it) },
    IntroPage(
        "Construisez vos habitudes",
        "Pas (via Health Connect), sport et eau ont chacun leur objectif dans le journal. Vos activités augmentent aussi votre budget calorique.",
    ) { HabitsArt(it) },
    IntroPage(
        "Faites grandir vos séries",
        "Chaque objectif atteint est coché dans le calendrier de l'historique. Enchaînez les jours pour battre votre meilleure série !",
    ) { StreakArt(it) },
)

@Composable
fun IntroScreen(onDone: () -> Unit) {
    val pager = rememberPagerState { PAGES.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == PAGES.lastIndex
    // On first launch the splash is still fading out: start the first scene only once it is gone.
    val ready = splashGone()
    AutoAdvance(pager, ready)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding().padding(16.dp)) {
        Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.CenterEnd) {
            if (!last) TextButton(onClick = onDone) { Text("Passer") }
        }
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            PageContent(PAGES[page], active = ready && pager.settledPage == page)
        }
        PageDots(pager)
        Spacer(Modifier.height(16.dp))
        WideButton(if (last) "C'est parti" else "Suivant", {
            if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
        })
    }
}

/** Moves to the next page after its dwell time (the last page waits for « C'est parti »). */
@Composable
private fun AutoAdvance(pager: PagerState, ready: Boolean) {
    LaunchedEffect(pager.settledPage, ready) {
        val page = pager.settledPage
        if (!ready || page == PAGES.lastIndex) return@LaunchedEffect
        delay(if (page == 0) WELCOME_DWELL_MS else PAGE_DWELL_MS)
        // Never pull the page from under a finger that is dragging it.
        if (!pager.isScrollInProgress) pager.animateScrollToPage(page + 1)
    }
}

@Composable
private fun PageContent(page: IntroPage, active: Boolean) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(ART_SIZE + 40.dp), contentAlignment = Alignment.Center) { page.art(active) }
        Spacer(Modifier.height(24.dp))
        Text(page.title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(page.text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PageDots(pager: PagerState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        repeat(PAGES.size) { i ->
            val selected = pager.currentPage == i
            Box(
                Modifier.size(if (selected) 10.dp else 8.dp).clip(CircleShape)
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}
