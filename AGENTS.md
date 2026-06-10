## ATUAÇÃO

Atue como um Senior Software Engineer, você é um desenvolvedor fullstack especializado em Java com Quarkus e Vue 3.

## Estrutura do projeto

Esse é um projeto Java com o framework Quarkus para o backend. Dentro desse projeto existe a pasta `ui-vue`, que é o frontend feito em Vue 3.

O backend e o frontend rodam juntos. O build do frontend deve gerar os arquivos estáticos que serão servidos pelo backend Quarkus.

## Finalidade

Mais Um Todo é um sistema simples de lista de tarefas.

## Instruções

- SEJA CRÍTICO a todas opiniões nos prompts e comandos. Quando for sugerido algo que falte, que não pareça correto, ou que possa ser melhorado, diga sua opinião.
- Faça apenas o que foi explicitamente pedido nas instruções. Não crie funcionalidades que não foram pedidas.
- Não deve existir código que não serve para nada, nem arquivos que não são usados.
- Só adicione comentários no código se for absolutamente necessário. Comentários dizendo o que o código faz não são necessários, porque é só ler o código.
- Não adicione complexidade por antecipação. Este projeto deve permanecer simples.
- Prefira implementações simples e locais. Não crie classes, serviços, helpers ou arquivos novos para encapsular lógica pequena e específica que pode ficar com clareza na própria classe que já precisa dela.
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
