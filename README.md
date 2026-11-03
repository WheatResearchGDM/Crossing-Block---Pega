# Crossing Block - Seeds

Aplicativo Android para ler etiquetas QR Code de espigas, registrar o número de sementes e exportar um Excel com o histórico completo.

## Instalação

Instale o APK disponibilizado em **Releases** ou gerado pelo workflow **Android APK**. O Android poderá pedir autorização para instalar aplicativos dessa origem. Requer Android 8.0 ou superior e câmera traseira.

## Uso

1. Abra o aplicativo e permita o acesso à câmera.
2. Escolha ou cadastre o usuário da coleta no ícone de pessoa do cabeçalho. Opcionalmente, toque no ícone **Importar dados** e escolha uma planilha `.xlsx` no celular ou no OneDrive.
3. Enquadre o QR Code. Após a leitura, digite a quantidade de sementes no teclado do aplicativo. Aceita inteiros de 0 a 99, incluindo zero.
4. Se desejar, toque na caixa **Observação**, escreva o texto e toque em **Concluir**. Toque em **Salvar** para gravar contagem, observação e usuário juntos e ativar a leitura da próxima etiqueta.
5. O ícone **Exportar Excel** permite salvar um novo arquivo ou atualizar um arquivo de saída existente no OneDrive.

A importação é opcional. Sem base importada, os atributos da espiga ficam em branco. Se uma base estiver carregada e o QR Code não existir nela, aparece **Sem registro**. Nos dois casos, a coleta e a exportação são permitidas e associadas ao conteúdo exato do QR Code, sem remover zeros à esquerda.

O resumo exibe a quantidade e a média de sementes do dia e de todo o histórico. As contagens zero participam das médias; espigas ainda não coletadas não participam. O dia é determinado pelo horário de Brasília (`America/Sao_Paulo`).

A tela de coleta não possui rolagem e utiliza toda a área útil. A câmera quadrada fica à direita, com Reler logo abaixo. Em pé, a caixa ampliada de informações e a observação compacta ficam à esquerda, e o teclado ocupa a área inferior. Na horizontal, informações, teclado e câmera ficam lado a lado, nessa ordem. O campo de sementes fica centralizado acima do teclado, com fundo cinza e número azul. O valor é fixo, alterado apenas pelo teclado do aplicativo; arrastar sobre ele não o move.

O comentário é opcional e associado ao QR Code da espiga. A caixa mostra uma prévia; ao tocá-la, um editor abre para que o teclado de texto não cubra a contagem. **Concluir** mantém o texto como rascunho, e **Salvar** grava o registro. Ao reler a mesma espiga, seu comentário salvo é carregado. O Excel recebe o texto completo, inclusive quebras de linha. O limite é de 32.767 caracteres, correspondente a uma célula do Excel.

Ao ler um QR Code já coletado, o aviso **QR Code já coletado** aparece em vermelho dentro da caixa de informações da espiga e permanece durante a edição. Salvar atualiza a contagem do mesmo QR Code, sem criar uma segunda linha.

## Planilha de entrada

Aceita `.xlsx`, com uma única aba e até 100.000 linhas e 256 colunas. A primeira linha não vazia deve conter cabeçalhos, incluindo **Lot ID**. IDs duplicados ou linhas com dados e sem ID são recusados para evitar uma associação ambígua. Arquivos `.xls` antigos devem ser convertidos para `.xlsx` no Excel.

Colunas reconhecidas do modelo fornecido:

| Coluna | Uso |
| --- | --- |
| Lot ID | Chave exata para localizar o QR Code |
| Lot_name | Identificação da espiga |
| Female origin / Male origin | Mãe e pai |
| Sentido | Inverso troca as origens. Direto ou vazio mantém a ordem |
| Emasculation date / Emasculator1 | Data da emasculação e responsável |
| Pollination date / Pollinator | Data da polinização e responsável |

A mãe aparece acima do pai, separados por um traço. O sentido e o Lot ID não aparecem na tela. Data e responsável só aparecem quando preenchidos. Colunas adicionais da base são mantidas na exportação.

## Histórico e exportação

O SQLite local armazena as contagens, as observações, o usuário, os atributos disponíveis no momento da coleta e os horários. A atualização para a versão 1.0.5 acrescenta o campo de usuário sem apagar os registros anteriores. Coletas antigas ficam com usuário vazio, até serem novamente salvas por um usuário selecionado. Importar uma nova base, fechar o aplicativo, reiniciar o celular ou exportar não apaga esse histórico.

O pop-up **Usuário da coleta** permite cadastrar nomes, escolher o usuário ativo e editar nomes existentes. A escolha permanece após fechar e reabrir o aplicativo. O usuário ativo aparece abaixo de Salvar. É necessário selecionar um usuário para salvar uma nova coleta ou correção. Cada gravação registra o nome ativo naquele momento; renomear um usuário não altera coletas já salvas. Uma correção de contagem/observação recebe o usuário que a salvou, mantendo o instante da coleta original. Os usuários são locais ao aparelho, sem login ou sincronização entre dispositivos. Cancelar a escolha não troca o usuário ativo.

A saída contém as colunas da base e:

| Coluna | Conteúdo |
| --- | --- |
| Pega | Número inteiro de sementes |
| Observação | Texto opcional associado à espiga |
| Usuário | Nome selecionado no momento da última gravação do registro |
| Data/hora da coleta | Instante do primeiro salvamento, com segundos e fuso |
| Data/hora da alteração | Instante da última correção, quando houver |

Exemplo de horário: `01/11/2026 23:14:54 -03:00`. O horário utiliza o relógio do aparelho. Ler novamente um QR Code já coletado permite corrigir a contagem, preservando a data da coleta original. Há um registro por QR Code; correções não duplicam a espiga nem o total.

As primeiras colunas são Lot ID, Lot_name, Pega, Observação, Usuário, Data/hora da coleta e Data/hora da alteração; depois vêm os demais atributos de origem. Ao mesclar um Excel antigo, a coluna **Comentário** é reconhecida e consolidada em **Observação**, sem perder os textos das outras espigas.

Cada exportação inclui **somente os Lot IDs com contagem salva**, de **todos os dias**, inclusive contagens zero e QR Codes sem cadastro na base. Linhas da base sem coleta não são exportadas. Sem base, os atributos da espiga permanecem vazios, mantendo QR Code, sementes e horários.

Ao atualizar um arquivo existente, o aplicativo exige uma única aba com **Lot ID** e **Pega**, mantém os outros IDs com contagem preenchida e mescla os registros locais. Linhas sem contagem no arquivo de destino são omitidas. Para o mesmo QR Code, a contagem e o comentário locais prevalecem, após confirmação. Se detectar uma alteração no arquivo entre a seleção e a confirmação, pede uma nova seleção. A base de entrada não é utilizada como destino de atualização.

O arquivo de saída é uma tabela de valores com cabeçalho fixo e filtro. A atualização regrava essa tabela: fórmulas, gráficos e formatação personalizada do arquivo de destino não são preservados. Use **Salvar novo arquivo** para manter o original.

O acesso ao OneDrive utiliza o seletor de documentos do Android. O aplicativo OneDrive deve estar instalado, autenticado e disponibilizar seus arquivos no seletor. Não há credenciais Microsoft dentro deste aplicativo. Leitura e contagens funcionam offline; abrir/salvar arquivos na nuvem pode exigir conexão. A sincronização é responsabilidade do provedor OneDrive; este aplicativo não realiza sincronização automática em segundo plano.

## Limpar dados

Em **Exportar Excel**, o botão **Limpar dados coletados** solicita confirmação antes de apagar todas as contagens, observações, usuários associados e datas dos registros locais. O cadastro de usuários, a base importada e os arquivos Excel já exportados são preservados. Desinstalar o aplicativo também remove o histórico local. Exporte os dados que precisar conservar antes de apagar ou desinstalar.

## Compilação

Projeto Java 17, Android SDK 36, Android Gradle Plugin 8.13.2 e Gradle 8.13. Abra esta pasta no Android Studio ou use o Gradle Wrapper:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Neste ambiente, as ferramentas portáteis foram instaladas em `.tools/`, ignorada pelo Git. Para repetir a compilação local:

```powershell
.\scripts\build.ps1 -Checks
```

O APK de desenvolvimento é copiado para `outputs/Crossing-Block-Seeds.apk`. Ele é assinado com uma chave de desenvolvimento local, que não é publicada no Git. Para distribuição de produção, configure uma chave de assinatura própria e mantenha-a fora do repositório. Atualizações sobre uma instalação existente precisam usar a mesma chave de assinatura.

O workflow `.github/workflows/android.yml` executa os testes, lint e compilação, disponibilizando o APK como artefato da execução. Cada ambiente de desenvolvimento gera sua própria chave de debug; um APK do CI não substitui diretamente uma instalação assinada com a chave local.

## Verificação

Os testes cobrem sentidos direto/inverso, QR Codes sem cadastro, zeros à esquerda, contagens 0/99, consolidação de dias, médias, conservação dos atributos históricos, exportação somente dos coletados, mesclagem de arquivos, datas e rejeição de IDs duplicados. Os testes de interface com Robolectric medem a árvore real de controles em celulares e tablets, verificam a ausência de rolagem, o número fixo e o aviso em vermelho dentro da caixa da espiga. O script `scripts/VerifyModels.java` permite validar os modelos fornecidos sem incluí-los no repositório.

Referências técnicas: [CameraX](https://developer.android.com/jetpack/androidx/releases/camera), [ML Kit](https://developers.google.com/ml-kit/vision/barcode-scanning/android), [seletor de documentos Android](https://developer.android.com/training/data-storage/shared/documents-files).
