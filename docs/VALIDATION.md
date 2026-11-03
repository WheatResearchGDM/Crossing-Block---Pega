# Verificação da versão 1.0.3

- `testDebugUnitTest`: 23 testes aprovados — 12 de dados e Excel, 3 de armazenamento/migração e 8 da interface com Robolectric.
- `lintDebug`: concluído sem erros. Há avisos de internacionalização, versões de dependências e APIs obsoletas.
- `assembleDebug`: APK gerado e assinado com a chave de desenvolvimento local.
- `apksigner verify`: assinatura APK v2 válida.
- `zipalign -c -P 16 4`: alinhamento do APK aprovado.
- Os dois modelos fornecidos foram lidos, com 2.299 espigas em cada um. Datas e os cruzamentos direto/inverso foram conferidos.
- Uma exportação com a base de 2.299 espigas, uma coleta conhecida com zero e uma desconhecida com 99 resultou em somente duas linhas de dados. Conferidos os números, QR Code com zeros à esquerda como texto, atributos desconhecidos vazios e ausência de Lot IDs sem coleta.
- A exportação foi relida por um leitor independente para conferir tipos, datas, cabeçalho fixo e filtro.

Os testes de layout usam a interface real, sem iniciar a câmera: celulares de 320×480, 360×640, 360×740 e 412×840; tablets de 800×1280 e 1200×1920; modo horizontal de 640×360 até 1920×1200; redimensionamento e área reservada às barras do sistema. Conferem todos os controles dentro da tela, ausência de contêineres de rolagem, câmera quadrada, utilização da largura e da parte inferior do tablet, e distribuição lado a lado no modo horizontal. Também verificam que os valores 0/99 não se deslocam após arrastar ou tentar rolar, que o aviso de QR Code já coletado fica em vermelho dentro da caixa da espiga durante a edição e que o editor de comentário mantém o texto associado ao QR Code correto.

Não havia dispositivo Android conectado. Os testes automatizados de interface e SQLite usam Robolectric, sem aparelho/emulador. Câmera e seletor OneDrive ainda precisam ser conferidos em um aparelho. Antes de usar em uma coleta real, confira também a instalação sobre a versão anterior, fechamento/reabertura do app, importação/exportação e cancelamento/confirmação da limpeza.

Os modelos de entrada, os registros utilizados na prévia, ferramentas locais e chaves de assinatura não foram publicados no repositório.

A migração do banco da versão 1 para a 2 foi testada com registros antigos, incluindo contagem zero, atributos e horários. O campo de comentário é acrescentado sem apagar ou recriar a tabela. A exportação com comentários foi validada por leitor independente, preservando acentos, texto multilinha e a formatação de quebra de linha.
