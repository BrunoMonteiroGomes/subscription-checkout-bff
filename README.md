# Subscription Checkout BFF

BFF (Backend For Frontend) do fluxo de checkout de assinaturas. Expõe dois endpoints:

| Etapa        | Endpoint                         | O que faz                                                                 |
|--------------|----------------------------------|---------------------------------------------------------------------------|
| Pré-checkout | `POST /operations/pre-checkout`  | Valida os produtos, devolve os meios de pagamento e cria um carrinho (`cartId`). |
| Checkout     | `POST /operations/checkout`      | Simula a cobrança do carrinho e devolve aprovação ou erro.                |

> Nenhum gateway real é chamado. Produtos, meios de pagamento e carrinhos são simulados **em memória**
> (`InMemoryProductClient`, `InMemoryPaymentMethodClient` e `InMemoryCartRepository`).
> Ao reiniciar a aplicação, os carrinhos são perdidos.

## 🚀 Forma mais fácil de testar (Docker, sem instalar nada)

Se você quer avaliar o fluxo completo (BFF + Frontend) **sem instalar Java, Maven, Node.js ou Angular CLI**, use o repositório de avaliação:

**https://github.com/BrunoMonteiroGomes/subscription-checkout-evaluation**

Ele contém um `docker-compose.yml` que baixa automaticamente este BFF e o frontend diretamente do GitHub, constrói as imagens e sobe os dois containers conectados.

### Como rodar

```bash
git clone https://github.com/BrunoMonteiroGomes/subscription-checkout-evaluation.git
cd subscription-checkout-evaluation
docker compose up --build
```

Depois de alguns minutos (o primeiro build baixa dependências Maven e npm), acesse:

**http://localhost:4200**

O frontend estará na porta `4200` e este BFF na porta `8080`. Para parar:

```bash
docker compose down
```

> Requisitos: apenas **Docker** instalado e em execução. Nada mais.

Se preferir rodar o BFF isoladamente (por exemplo, para testar os endpoints via `curl`), siga as instruções de **Pré-requisitos** e **Como subir a API** abaixo.

## Pré-requisitos

- **Java 25** (definido em `java.version` no `pom.xml`)
- Não é necessário instalar Maven: use o wrapper `./mvnw` incluído no projeto.
- Não é necessário ter Redis para o fluxo descrito aqui (o armazenamento é em memória).
- Docker é necessário **apenas** se for usar a opção 3 abaixo (Redis via Testcontainers).

Confira a versão do Java:

```bash
java -version
```

## Como subir a API

A API sobe na porta **8080** (padrão do Spring Boot).

### Opção 1 — Maven (recomendada)

```bash
./mvnw spring-boot:run
```

### Opção 2 — Gerar o JAR e executar

```bash
./mvnw clean package
java -jar target/subscription-checkout-bff-0.0.1-SNAPSHOT.jar
```

### Opção 3 — Com Redis via Testcontainers (requer Docker)

Execute a classe `TestSubscriptionCheckoutBffApplication` (em `src/test/java`) pela IDE
ou com:

```bash
./mvnw spring-boot:test-run
```

### Para trocar a porta

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=9090
```

A API está pronta quando o log exibir `Started SubscriptionCheckoutBffApplication`.

## Dados simulados disponíveis

**Produtos** (use o `slug` no pré-checkout):

| slug      | Nome          | Preço (BRL) |
|-----------|---------------|-------------|
| `basico`  | Plano Básico  | 19.90       |
| `premium` | Plano Premium | 39.90       |
| `familia` | Plano Família | 59.90       |

**Meios de pagamento** (use exatamente estes nomes no checkout):

- `Cartão de Crédito`
- `Cartão de Débito`
- `Pix`

## Como usar (passo a passo)

### 1. Pré-checkout — criar o carrinho

```bash
curl -s -X POST http://localhost:8080/operations/pre-checkout \
  -H "Content-Type: application/json" \
  -d '{"products": [{"slug": "premium"}]}'
```

Resposta (`200 OK`):

```json
{
  "cartId": "d2b3c1e4-5f6a-4b7c-8d9e-0a1b2c3d4e5f",
  "products": [
    {
      "slug": "premium",
      "name": "Plano Premium",
      "description": "Assinatura mensal com catálogo completo",
      "price": 39.90,
      "currency": "BRL"
    }
  ],
  "paymentMethods": [
    { "name": "Cartão de Crédito" },
    { "name": "Cartão de Débito" },
    { "name": "Pix" }
  ]
}
```

Copie o valor de `cartId`: ele é necessário no checkout.

Para facilitar, você pode guardá-lo em uma variável (requer `jq`):

```bash
CART_ID=$(curl -s -X POST http://localhost:8080/operations/pre-checkout \
  -H "Content-Type: application/json" \
  -d '{"products": [{"slug": "premium"}]}' | jq -r .cartId)
```

> **Importante:** cada carrinho só pode ser aprovado **uma vez**. Para repetir um teste
> de sucesso, faça um novo pré-checkout e use o novo `cartId`.
> Já uma recusa **não** aprova o carrinho, então o mesmo `cartId` pode ser reutilizado para tentar de novo.

### 2. Checkout — simular **SUCESSO**

#### Sucesso com cartão (qualquer número que **não** termine em `0002`)

```bash
curl -s -i -X POST http://localhost:8080/operations/checkout \
  -H "Content-Type: application/json" \
  -d '{
    "cartId": "'"$CART_ID"'",
    "paymentMethod": {
      "name": "Cartão de Crédito",
      "card": {
        "holderName": "Maria Silva",
        "number": "4111 1111 1111 1111",
        "expiry": "12/30",
        "cvv": "123"
      }
    }
  }'
```

Resposta (`200 OK`):

```json
{
  "cartId": "d2b3c1e4-5f6a-4b7c-8d9e-0a1b2c3d4e5f",
  "status": "APROVADO",
  "paidAt": "2026-10-29T22:10:00.000Z",
  "payment": {
    "method": "Cartão de Crédito",
    "lastDigits": "1111",
    "amount": 39.90,
    "currency": "BRL"
  },
  "products": [
    { "slug": "premium", "name": "Plano Premium", "price": 39.90 }
  ]
}
```

#### Sucesso com Pix (não envia `card`)

```bash
curl -s -i -X POST http://localhost:8080/operations/checkout \
  -H "Content-Type: application/json" \
  -d '{
    "cartId": "'"$CART_ID"'",
    "paymentMethod": { "name": "Pix" }
  }'
```

Resposta (`200 OK`): igual à anterior, mas `payment.method` é `"Pix"` e `payment.lastDigits`
traz o código Pix simulado (ex.: `"PIX-1A2B3C4D5E6F"`).

### 3. Checkout — simular **RECUSA**

Use um cartão cujo número **termine em `0002`** (ex.: `4000 0000 0000 0002`).
Os espaços e hífens são ignorados; só os dígitos contam.

```bash
curl -s -i -X POST http://localhost:8080/operations/checkout \
  -H "Content-Type: application/json" \
  -d '{
    "cartId": "'"$CART_ID"'",
    "paymentMethod": {
      "name": "Cartão de Crédito",
      "card": {
        "holderName": "Maria Silva",
        "number": "4000 0000 0000 0002",
        "expiry": "12/30",
        "cvv": "123"
      }
    }
  }'
```

Resposta (`402 Payment Required`):

```json
{
  "displayType": "BLOCK",
  "errorCode": "PAYMENT_DECLINED",
  "frontendError": {
    "code": "P-1001",
    "title": "Pagamento não aprovado",
    "description": "Não foi possível concluir o pagamento. Tente novamente ou escolha outro meio.",
    "primaryButton": { "label": "Tentar novamente", "url": "RETRY" },
    "secondaryButton": { "label": "Trocar meio de pagamento", "url": "/checkout/pagamento" }
  }
}
```

### Resumo das regras de simulação

| Cenário                                             | Como simular                                              | Resultado                         |
|-----------------------------------------------------|-----------------------------------------------------------|-----------------------------------|
| Aprovação com cartão                                | Cartão de Crédito/Débito com número **não** terminado em `0002` | `200` — `status: "APROVADO"`      |
| Aprovação com Pix                                   | `paymentMethod.name = "Pix"`, sem `card`                  | `200` — código Pix simulado       |
| **Recusa**                                          | Cartão terminado em **`0002`**                            | `402` — `PAYMENT_DECLINED` (`P-1001`) |

## Outros erros que podem ser simulados

| Situação                                              | Como reproduzir                                          | HTTP | `errorCode`              |
|-------------------------------------------------------|----------------------------------------------------------|------|--------------------------|
| Produto inexistente no pré-checkout                   | `{"products": [{"slug": "inexistente"}]}`                | 404  | `PRODUCT_NOT_FOUND`      |
| Carrinho inexistente                                  | Checkout com um `cartId` qualquer/desconhecido           | 404  | `CART_NOT_FOUND`         |
| Carrinho já pago                                      | Repetir o checkout aprovado com o mesmo `cartId`         | 409  | `CART_ALREADY_APPROVED`  |
| Meio de pagamento inválido                            | `paymentMethod.name` fora da lista (ex.: `"Boleto"`)     | 422  | `INVALID_PAYMENT_METHOD` |
| Cartão sem dados                                      | `Cartão de Crédito`/`Débito` sem o objeto `card`         | 422  | `CARD_REQUIRED`          |
| Cartão inválido                                       | `card.number` com menos de 4 dígitos                     | 422  | `INVALID_CARD`           |
| Corpo inválido (campo obrigatório ausente/em branco)  | Ex.: `cartId` vazio ou `products` vazio                  | 400  | `VALIDATION_ERROR`       |

## Testes

```bash
./mvnw test
```

> Os testes de contexto (`SubscriptionCheckoutBffApplicationTests`) usam Testcontainers com Redis, portanto
> exigem Docker em execução.
