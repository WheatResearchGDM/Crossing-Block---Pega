# Verificação da versão 1.0.0

- `testDebugUnitTest`: nove testes, sem falhas ou erros.
- `lintDebug`: concluído sem erros. Há avisos de internacionalização, versões de dependências e APIs obsoletas.
- `assembleDebug`: APK gerado e assinado com a chave de desenvolvimento local.
- `apksigner verify`: assinatura APK v2 válida.
- `zipalign -c -P 16 4`: alinhamento do APK aprovado.
- Os dois modelos fornecidos foram lidos, com 2.299 espigas em cada um. Datas e os cruzamentos direto/inverso foram conferidos.
- Uma exportação com os modelos e um QR Code desconhecido resultou em 2.300 linhas de dados. Conferidos zero e 99 como números, QR Code com zeros à esquerda como texto, atributos desconhecidos vazios e linhas sem coleta com Pega em branco.
- A exportação foi relida por um leitor independente para conferir tipos, datas, cabeçalho fixo e filtro.

Não havia dispositivo Android conectado. Não foram realizados testes em aparelho/emulador da câmera, interface, persistência SQLite e do seletor OneDrive. Antes de usar em uma coleta real, instale o APK e confira: leitura da etiqueta, captura 0/99, fechamento/reabertura do app, contagens e médias, importação, exportação local, exportação no OneDrive e cancelamento/confirmação da limpeza.

Os modelos de entrada, os registros utilizados na prévia, ferramentas locais e chaves de assinatura não foram publicados no repositório.
