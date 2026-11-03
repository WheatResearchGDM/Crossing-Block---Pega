# Verificação da versão 1.0.5

- `testDebugUnitTest`: 32 testes — 13 de dados e Excel, 4 de armazenamento/migração, 2 de usuários e 13 da interface com Robolectric.
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

A versão 1.0.4 acrescenta testes de exibição completa dos valores 349, 3499 e 123456 em uma linha. Também verifica 349/7 e o rótulo Reler em telas xhdpi de 738×1600, 640×1280 e 800×1600, sem quebra de linha, reticências ou caracteres cortados.

A versão 1.0.5 verifica câmera à direita, campo de sementes centralizado, observação limitada a duas linhas e caixa de informações ampliada no tablet. O fluxo de usuário cobre cadastro no pop-up, seleção, preservação do rascunho e associação do nome à coleta. Sem usuário selecionado, Salvar abre a escolha sem perder a contagem ou a observação. O cadastro e a escolha persistem após reabrir; nomes vazios, longos e duplicados são recusados.

A migração da versão 2 para a 3 mantém comentários, contagens e horários, deixando o usuário antigo vazio. Os testes da versão 1 também passam pela migração até a versão 3. A exportação preserva o nome salvo na coleta, reconhece a antiga coluna Comentário e a consolida em Observação. Renomear um usuário não modifica registros anteriores. O ensaio dos modelos de 2.299 linhas inclui observação e usuário, mantendo apenas as duas espigas coletadas no resultado.
