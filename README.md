# Crossing Block - Seeds

Aplicativo Android para ler etiquetas QR Code de espigas, registrar o número de sementes e exportar um Excel com o histórico completo.

## Instalação

Instale o APK disponibilizado em **Releases** ou gerado pelo workflow **Android APK**. O Android poderá pedir autorização para instalar aplicativos dessa origem. Requer Android 8.0 ou superior e câmera traseira.

## Uso

1. Abra o aplicativo e permita o acesso à câmera.
2. Opcionalmente, toque no ícone **Importar dados** do cabeçalho e escolha uma planilha `.xlsx` no celular ou no OneDrive.
3. Enquadre o QR Code. Após a leitura, digite a quantidade de sementes no teclado do aplicativo. Aceita inteiros de 0 a 99, incluindo zero.
4. Toque em **Salvar**. A contagem é gravada localmente e a leitura da próxima etiqueta é ativada automaticamente.
5. O ícone **Exportar Excel** permite salvar um novo arquivo ou atualizar um arquivo de saída existente no OneDrive.

A importação é opcional. Sem base importada, os atributos da espiga ficam em branco. Se uma base estiver carregada e o QR Code não existir nela, aparece **Sem registro**. Nos dois casos, a coleta e a exportação são permitidas e associadas ao conteúdo exato do QR Code, sem remover zeros à esquerda.

O resumo exibe a quantidade e a média de sementes do dia e de todo o histórico. As contagens zero participam das médias; espigas ainda não coletadas não participam. O dia é determinado pelo horário de Brasília (`America/Sao_Paulo`).

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

O SQLite local armazena as contagens, os atributos disponíveis no momento da coleta e os horários. Importar uma nova base, fechar o aplicativo, reiniciar o celular ou exportar não apaga esse histórico.

A saída contém as colunas da base e:

| Coluna | Conteúdo |
| --- | --- |
| Pega | Número inteiro de sementes |
| Data/hora da coleta | Instante do primeiro salvamento, com segundos e fuso |
| Data/hora da alteração | Instante da última correção, quando houver |

Exemplo de horário: `01/11/2026 23:14:54 -03:00`. O horário utiliza o relógio do aparelho. Ler novamente um QR Code já coletado permite corrigir a contagem, preservando a data da coleta original. Há um registro por QR Code; correções não duplicam a espiga nem o total.

Cada exportação inclui o histórico de **todos os dias**, inclusive QR Codes sem cadastro e as linhas da base ainda sem coleta, cujo campo Pega permanece vazio. Sem base, exporta as linhas efetivamente coletadas com seus QR Codes, sementes e horários.

Ao atualizar um arquivo existente, o aplicativo exige uma única aba com **Lot ID** e **Pega**, mantém os outros IDs e mescla os registros locais. Para o mesmo QR Code, a contagem local prevalece, após confirmação. Se detectar uma alteração no arquivo entre a seleção e a confirmação, pede uma nova seleção. A base de entrada não é utilizada como destino de atualização.

O arquivo de saída é uma tabela de valores com cabeçalho fixo e filtro. A atualização regrava essa tabela: fórmulas, gráficos e formatação personalizada do arquivo de destino não são preservados. Use **Salvar novo arquivo** para manter o original.

O acesso ao OneDrive utiliza o seletor de documentos do Android. O aplicativo OneDrive deve estar instalado, autenticado e disponibilizar seus arquivos no seletor. Não há credenciais Microsoft dentro deste aplicativo. Leitura e contagens funcionam offline; abrir/salvar arquivos na nuvem pode exigir conexão. A sincronização é responsabilidade do provedor OneDrive; este aplicativo não realiza sincronização automática em segundo plano.

## Limpar dados

Em **Exportar Excel**, o botão **Limpar dados coletados** solicita confirmação antes de apagar todas as contagens e datas locais. A base importada e os arquivos Excel já exportados são preservados. Desinstalar o aplicativo também remove o histórico local. Exporte os dados que precisar conservar antes de apagar ou desinstalar.

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

Os testes de unidade cobrem sentidos direto/inverso, QR Codes sem cadastro, zeros à esquerda, contagens 0/99, consolidação de dias, médias, conservação dos atributos históricos, mesclagem de arquivos, datas e rejeição de IDs duplicados. O script `scripts/VerifyModels.java` permite validar os modelos fornecidos sem incluí-los no repositório.

Referências técnicas: [CameraX](https://developer.android.com/jetpack/androidx/releases/camera), [ML Kit](https://developers.google.com/ml-kit/vision/barcode-scanning/android), [seletor de documentos Android](https://developer.android.com/training/data-storage/shared/documents-files).
