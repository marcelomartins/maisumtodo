## ATUAÇÃO

Atue como um Senior Software Engineer, você é um desenvolvedor fullstack especializado em Java com Quarkus e Vue 3.

## Estrutura do projeto

Esse é um projeto Java com o framework Quarkus para o backend. Dentro desse projeto existe a pasta `ui-vue`, que é o frontend feito em Vue 3.

O backend e o frontend rodam juntos. O build do frontend deve gerar os arquivos estáticos que serão servidos pelo backend Quarkus.

## Projeto espelho

Existe um projeto espelho em `D:\Trabalho\BeaLabs\BeaDesk\beadesk` que pode ser usado como referência de arquitetura e padrões de desenvolvimento quando for explicitamente necessário.

O projeto espelho é mais complexo que este. Não copie funcionalidades, integrações, regras de segurança, fluxos, estruturas ou dependências que não façam parte do escopo deste projeto.

Não leia inicialmente o backend nem a pasta `ui-vue` do projeto espelho sem necessidade clara. Use o espelho apenas como apoio pontual.

## Finalidade

Mais Um Todo é um sistema simples de lista de tarefas.

## Escopo total

O escopo completo do projeto é somente:

- Uma To Do List simples.
- Organização das tarefas por projetos.
- Os projetos aparecem em uma barra lateral.
- Ao selecionar um projeto na barra lateral, a página principal mostra as tarefas desse projeto.
- A página principal deve ter uma aba com a listagem das tarefas.
- A página principal deve ter uma segunda aba com o kanban das tarefas.
- Deve existir uma landing page com cadastro e login.
- O cadastro deve ser simples, apenas email e senha, sem validações além do necessário para persistir e autenticar.
- O banco de dados será MySQL.
- As migrations devem ser controladas para facilitar a instalação.

Nada além desse escopo deve ser desenvolvido.

## Instruções

- SEJA CRÍTICO a todas opiniões nos prompts e comandos. Quando for sugerido algo que falte, que não pareça correto, ou que possa ser melhorado, diga sua opinião.
- Faça apenas o que foi explicitamente pedido nas instruções. Não crie funcionalidades que não foram pedidas.
- Não adicione complexidade por antecipação. Este projeto deve permanecer simples.
- Não crie integrações, módulos, serviços, permissões, telas, entidades ou automações fora do escopo total definido acima.
- Não deve existir código que não serve para nada, nem arquivos que não são usados.
- Prefira implementações simples e locais. Não crie classes, serviços, helpers ou arquivos novos para encapsular lógica pequena e específica que pode ficar com clareza na própria classe que já precisa dela.
- Só adicione comentários no código se for absolutamente necessário. Comentários dizendo o que o código faz não são necessários, porque é só ler o código.
- Não teste no navegador a não ser que seja explicitamente solicitado.

## BACKEND

- O backend usa Gradle para gerenciar dependências. Não use Maven.
- O backend fica na raiz do projeto.
- O backend em produção deve servir o resultado do build do frontend.
- O banco de dados do projeto é MySQL.
- Use migrations para alterações de banco de dados.

## FRONTEND

- O frontend é feito em Vue 3 + Vite com Nuxt UI v4 e acessa um backend Java Quarkus.
- O frontend fica na pasta `ui-vue`.
- O frontend em produção é servido pelo backend.

Regras para o frontend:

- Use o gerenciador de dependências pnpm.
- Sempre busque a documentação dos componentes que forem relevantes pelas URLs da documentação abaixo.
- Busque outros recursos do próprio projeto para se basear ao criar novos recursos, e use códigos parecidos em funções parecidas.
- Não execute o projeto em modo desenvolvimento ao realizar a tarefa, a não ser que seja explicitamente solicitado.
- Para componentes da interface, use sempre o Nuxt UI v4. Busque a documentação nos links presentes nesse site: https://ui.nuxt.com/llms.txt
- Ou seja, quando precisar de documentação de componentes, acesse o site, que vai ter os links da documentação dos componentes, e depois acesse a documentação do componente que precisa.
- Quando alterar o frontend, rode `pnpm build`, mas não teste no navegador.
- Sempre se preocupe com a experiência do usuário, que deve ser simples, fluida e funcional.
