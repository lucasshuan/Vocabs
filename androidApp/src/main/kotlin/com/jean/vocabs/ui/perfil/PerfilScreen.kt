package com.jean.vocabs.ui.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.shared.domain.ResumoCurso
import com.jean.vocabs.ui.components.BandeiraCircular
import com.jean.vocabs.ui.components.CaixaTracejada
import com.jean.vocabs.ui.components.CartaoDaTela
import com.jean.vocabs.ui.components.ChevronDeLinha
import com.jean.vocabs.ui.components.DiscoDeIcone
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.LinhaDeLista
import com.jean.vocabs.ui.components.LinhaDeUsoDeIa
import com.jean.vocabs.ui.components.RotuloDeSecao
import com.jean.vocabs.ui.idiomas.idiomaDe

/**
 * Tela 07 do handoff — "Você".
 *
 * Total antes da quebra: sequência e estoque são hábito, e hábito não pertence a
 * um curso. Cada linha de idioma abre o "Seu progresso" **daquele** curso, com
 * semana, quota e estoque próprios — e sem trocar o curso aberto por baixo de
 * quem só queria olhar.
 *
 * A troca de idioma saiu daqui: ela agora é o deslize da Início. O que sobrou
 * desse assunto nesta tela é adicionar, remover e o idioma-base.
 */
@Composable
fun PerfilScreen(
    aoAbrirProgresso: (String) -> Unit,
    aoAbrirConfiguracoes: () -> Unit,
    aoAbrirNovoIdioma: () -> Unit,
    aoTrocarIdiomaNativo: () -> Unit,
    vm: PerfilViewModel = viewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val nativo = idiomaDe(estado.par.nativo)

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Text("Você", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 22.dp))

        CartaoDaTela(recheio = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth()) {
            Text("No total", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "somando todos os idiomas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
                NumeroDoResumo(
                    valor = "${estado.totalDeDominadas}",
                    rotulo = if (estado.totalDeDominadas == 1) "dominada" else "dominadas",
                    cor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f),
                )
                DivisorVertical()
                NumeroDoResumo(
                    valor = "${estado.diasSeguidos}",
                    rotulo = if (estado.diasSeguidos == 1) "dia seguido" else "dias seguidos",
                    cor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                DivisorVertical()
                NumeroDoResumo(
                    valor = "${estado.totalDeFichas}",
                    rotulo = if (estado.totalDeFichas == 1) "ficha" else "fichas",
                    cor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        ProgressoPorIdioma(
            cursos = estado.cursos,
            aoAbrir = aoAbrirProgresso,
            aoAdicionar = aoAbrirNovoIdioma,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        LinhaDeLista(
            aoClicar = aoTrocarIdiomaNativo,
            inicio = { BandeiraCircular(nativo, tamanho = 26.dp) },
            fim = { PilulaTrocar() },
        ) {
            Text("Meu idioma", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(nativo.nome, style = MaterialTheme.typography.titleSmall)
        }

        LinhaDeUsoDeIa(usadas = estado.usoIa.usadas, limite = estado.usoIa.limite)

        LinhaDeLista(
            titulo = "Configurações",
            aoClicar = aoAbrirConfiguracoes,
            inicio = { DiscoDeIcone(Icones.Engrenagem, null, cor = MaterialTheme.colorScheme.onSurfaceVariant, fundo = MaterialTheme.colorScheme.surfaceVariant) },
            fim = { ChevronDeLinha() },
        )
        Spacer(Modifier.navigationBarsPadding().height(110.dp))
    }
}

/**
 * A lista de cursos numa caixa com rolagem própria.
 *
 * Sem o teto de altura, quem estuda seis idiomas empurraria "Meu idioma" e
 * "Configurações" para fora da primeira tela — e essas linhas são justamente as
 * que ninguém procura rolando, porque não mudam nunca.
 */
@Composable
private fun ProgressoPorIdioma(
    cursos: List<ResumoCurso>,
    aoAbrir: (String) -> Unit,
    aoAdicionar: () -> Unit,
) {
    val cores = MaterialTheme.colorScheme
    val rolavel = cursos.size > 3

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            RotuloDeSecao("Progresso por idioma", Modifier.weight(1f))
            if (rolavel) {
                Text("role para ver", style = MaterialTheme.typography.bodySmall, color = cores.outline)
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(cores.surfaceVariant, RoundedCornerShape(22.dp))
                .heightIn(max = ALTURA_MAXIMA_DA_LISTA)
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
        ) {
            cursos.forEach { curso ->
                LinhaDeCurso(curso) { aoAbrir(curso.par.alvo) }
            }
            CaixaTracejada(
                modifier = Modifier.fillMaxWidth(),
                aoClicar = aoAdicionar,
                recheio = PaddingValues(horizontal = 15.dp, vertical = 11.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(30.dp).background(cores.surface, CircleShape),
                    ) {
                        Icon(Icones.Mais, null, tint = cores.primary, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = "Adicionar idioma",
                        style = MaterialTheme.typography.titleSmall,
                        color = cores.primary,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LinhaDeCurso(curso: ResumoCurso, aoClicar: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    val fracao = if (curso.total == 0) 0f else curso.dominadas.toFloat() / curso.total

    LinhaDeLista(
        aoClicar = aoClicar,
        inicio = { BandeiraCircular(idiomaDe(curso.par.alvo), tamanho = 30.dp) },
        fim = { ChevronDeLinha() },
    ) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = idiomaDe(curso.par.alvo).nome,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${curso.dominadas} de ${curso.total}",
                style = MaterialTheme.typography.bodySmall,
                color = cores.onSurfaceVariant,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 5.dp)
                .height(6.dp)
                .background(cores.outlineVariant, CircleShape),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fracao)
                    .height(6.dp)
                    .background(cores.tertiary, CircleShape),
            )
        }
    }
}

@Composable
private fun PilulaTrocar() {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 11.dp, end = 7.dp, top = 7.dp, bottom = 7.dp),
        ) {
            Text(
                text = "trocar",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector = Icones.Avancar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun NumeroDoResumo(valor: String, rotulo: String, cor: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(valor, style = MaterialTheme.typography.headlineMedium, color = cor)
        Text(
            text = rotulo,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun DivisorVertical() {
    Box(Modifier.width(1.dp).height(34.dp).background(MaterialTheme.colorScheme.outlineVariant))
}

/** Três linhas e meia: a meia diz que há mais, e o rodapé de conta continua na tela. */
private val ALTURA_MAXIMA_DA_LISTA = 232.dp
