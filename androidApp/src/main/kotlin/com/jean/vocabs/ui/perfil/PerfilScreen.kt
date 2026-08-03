package com.jean.vocabs.ui.perfil

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.contracts.Idioma
import com.jean.vocabs.shared.domain.ResumoCurso
import com.jean.vocabs.ui.components.BandeiraCircular
import com.jean.vocabs.ui.components.CartaoDaTela
import com.jean.vocabs.ui.components.ChevronDeLinha
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.LinhaDeLista
import com.jean.vocabs.ui.components.LinhaDeUsoDeIa
import com.jean.vocabs.ui.components.RotuloDeSecao
import com.jean.vocabs.ui.components.contornoDeCartao
import com.jean.vocabs.ui.idiomas.idiomaDe
import java.io.File

/**
 * Tela 5a do handoff — "Você".
 *
 * É a aba Perfil, e o que ela mostra é com que idiomas você está: o de partida,
 * a faixa de cursos e a porta para o progresso do curso aberto. Progresso e
 * Configurações são páginas de dentro, abertas daqui.
 */
@Composable
fun PerfilScreen(
    aoAbrirProgresso: () -> Unit,
    aoAbrirConfiguracoes: () -> Unit,
    aoAbrirNovoIdioma: () -> Unit,
    aoTrocarIdiomaNativo: () -> Unit,
    vm: PerfilViewModel = viewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val nativo = idiomaDe(estado.par.nativo)
    val alvo = idiomaDe(estado.par.alvo)

    Column(
        verticalArrangement = Arrangement.spacedBy(13.dp),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding(),
    ) {
        Text(
            text = "Você",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp),
        )

        LinhaDeLista(
            aoClicar = aoTrocarIdiomaNativo,
            inicio = { BandeiraCircular(nativo, tamanho = 30.dp) },
            fim = { PilulaTrocar() },
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            Text("Meu idioma", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(nativo.nome, style = MaterialTheme.typography.titleSmall)
        }

        FaixaDeCursos(
            cursos = estado.cursos,
            ativo = estado.par.alvo,
            aoEscolher = vm::abrirCurso,
            aoAdicionar = aoAbrirNovoIdioma,
        )

        CartaoDaTela(
            aoClicar = aoAbrirProgresso,
            recheio = PaddingValues(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Seu progresso", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "no seu ${alvo.nome.lowercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icones.Avancar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
                NumeroDoResumo(
                    valor = "${estado.cursoAtual?.dominadas ?: 0}",
                    rotulo = "dominadas",
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
                    valor = "${estado.quota.feita}/${estado.quota.total}",
                    rotulo = "quota de hoje",
                    cor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            LinhaDeUsoDeIa(usadas = estado.usoIa.usadas, limite = estado.usoIa.limite)

            LinhaDeLista(
                titulo = "Configurações",
                aoClicar = aoAbrirConfiguracoes,
                fim = { ChevronDeLinha() },
            )

            LinhaDeLista(
                titulo = "Exportar meus dados",
                detalhe = if (estado.exportando) "preparando ZIP…" else null,
                aoClicar = {
                    vm.exportar(
                        aoPronto = { arquivo -> compartilhar(contexto, arquivo) },
                        aoErro = { Toast.makeText(contexto, it, Toast.LENGTH_LONG).show() },
                    )
                },
                fim = {
                    if (estado.exportando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else ChevronDeLinha()
                },
            )
        }
        Spacer(Modifier.navigationBarsPadding().height(110.dp))
    }
}

/**
 * A faixa de cursos, sangrando até as bordas.
 *
 * A margem negativa é a afordância: o último cartão fica cortado pela borda da
 * tela, e é isso que diz que a faixa anda. Um `padding` normal encaixaria todos
 * os cartões visíveis dentro da margem e a rolagem viraria segredo.
 */
@Composable
private fun FaixaDeCursos(
    cursos: List<ResumoCurso>,
    ativo: String,
    aoEscolher: (String) -> Unit,
    aoAdicionar: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        ) {
            RotuloDeSecao("Aprendendo", Modifier.weight(1f))
            Text(
                text = "deslize para ver",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 2.dp),
        ) {
            cursos.forEach { curso ->
                CartaoDeCurso(
                    idioma = idiomaDe(curso.par.alvo),
                    contagem = "${curso.dominadas} de ${curso.total}",
                    selecionado = curso.par.alvo == ativo,
                    aoClicar = { aoEscolher(curso.par.alvo) },
                )
            }
            CartaoDeNovoIdioma(aoAdicionar)
        }
    }
}

@Composable
private fun CartaoDeCurso(
    idioma: Idioma,
    contagem: String,
    selecionado: Boolean,
    aoClicar: () -> Unit,
) {
    val cores = MaterialTheme.colorScheme
    Surface(
        onClick = aoClicar,
        shape = RoundedCornerShape(20.dp),
        color = if (selecionado) cores.secondaryContainer else cores.surface,
        border = if (selecionado) null else contornoDeCartao(),
        modifier = Modifier
            .width(104.dp)
            .then(if (selecionado) Modifier.border(2.dp, cores.primary, RoundedCornerShape(20.dp)) else Modifier),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
        ) {
            BandeiraCircular(idioma, tamanho = 40.dp)
            Text(
                text = idioma.nome,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = contagem,
                style = MaterialTheme.typography.bodySmall,
                color = if (selecionado) cores.primary else cores.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CartaoDeNovoIdioma(aoClicar: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    Surface(
        onClick = aoClicar,
        shape = RoundedCornerShape(20.dp),
        color = cores.background,
        border = androidx.compose.foundation.BorderStroke(1.dp, cores.outline),
        modifier = Modifier.width(104.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(40.dp).clip(CircleShape).background(cores.surfaceVariant),
            ) {
                Icon(Icones.Mais, null, tint = cores.primary, modifier = Modifier.size(22.dp))
            }
            Text("Novo", style = MaterialTheme.typography.titleSmall, color = cores.primary)
            Text("idioma", style = MaterialTheme.typography.bodySmall, color = cores.outline)
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

private fun compartilhar(contexto: android.content.Context, arquivo: File) {
    val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", arquivo)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    contexto.startActivity(Intent.createChooser(intent, "Exportar dados da Tagarara"))
}
