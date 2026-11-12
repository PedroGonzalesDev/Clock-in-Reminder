# Clock in Reminder — Contexto de Projeto para o Antigravity

Este documento resume o estado atual do projeto, arquitetura, regras de negócio e instruções para que o agente **Antigravity** (ou qualquer desenvolvedor) no computador pessoal possa dar continuidade ao desenvolvimento sem perder nenhum contexto.

---

## 📌 Informações do Repositório

- **Repositório GitHub:** `https://github.com/PedroGonzalesDev/Clock-in-Reminder.git`
- **Branch Principal:** `main`
- **Status do Build:** 100% dos testes unitários passando (`.\gradlew.bat testDebugUnitTest`).
- **Versão Atual do App:** `versionCode = 3`, `versionName = "1.0.2"`.

---

## 🏗️ Stack Tecnológica & Arquitetura

- **Linguagem:** Kotlin 1.9.24 (JVM 17)
- **UI:** Jetpack Compose + Material 3 (Design System escuro estilo painel de instrumentos)
- **Banco de Dados Local:** Room 2.6.1 (SQLite local, 100% offline, zero dependência de backend)
- **Arquitetura:** MVVM + Clean Architecture + Repository Pattern
- **Exportação:** Gerador nativo de relatórios em **PDF** (`PdfDocument`), **Excel** (XML Spreadsheet 2003 compatível) e **CSV**
- **Agendador de Alarmes:** `AlarmManager` com `ExactAndAllowWhileIdle` + `BroadcastReceiver`

---

## 💼 Regras de Negócio Implementadas

1. **Regime de Trabalho (PJ vs CLT):**
   - Configuração de Hora Extra em Dias Úteis:
     - **Modo PJ (0%):** Horas excedentes em dias úteis são pagas a **100% da hora base** (R$ 25,74/h), sem adicional de 50%.
     - **Modo CLT (+50%):** Horas excedentes em dias úteis recebem adicional de 50%.
   - **Sábado:** Adicional de 60% sobre a hora base.
   - **Domingo, Feriados e Folgas:** Adicional de 100% sobre a hora base.
   - **Adicional Noturno:** 20% para horas trabalhadas entre 22:00 e 05:00, com contagem legal reduzida de 52m30s.

2. **Registro Manual de Ponto:**
   - Substituição direta de dígitos (sem necessidade de apagar com backspace).
   - Salto automático para o campo de minutos ao digitar os 2 dígitos da hora.
   - Memória de contexto (Local: Empresa / Cliente / Home Office; Situação: Entrada / Intervalo / Saída).
   - Concatenação automática de múltiplas observações de batidas separadas por `/`.

3. **Tabelas e Relatórios:**
   - Suporte ilimitado a batidas diárias (não limitado a 4 batidas).
   - Coluna dinâmica: `Extra Útil` / `Ex.50%` e resumo com `Extras Dias Úteis (Valor Normal)` quando em modo PJ.

---

## 🔑 Assinatura Digital do APK (`debug.keystore`)

> [!IMPORTANT]
> O arquivo `debug.keystore` oficial está commitado na **raiz do repositório** e referenciado em `app/build.gradle.kts`.
> **Motivo:** Isso garante que qualquer APK compilado em qualquer computador (trabalho, pessoal ou CI) terá rigorosamente a **mesma assinatura digital**, permitindo atualizar o aplicativo no smartphone Android do usuário **sem conflito de pacotes e sem perder nenhum dado já registrado no banco SQLite**.

---

## 🚀 Como Executar e Compilar no Computador Pessoal

1. **Clonar o Repositório:**
   ```powershell
   git clone https://github.com/PedroGonzalesDev/Clock-in-Reminder.git
   cd Clock-in-Reminder
   ```

2. **Executar Testes Unitários:**
   ```powershell
   .\gradlew.bat testDebugUnitTest
   ```

3. **Compilar Novo APK (Debug):**
   ```powershell
   .\gradlew.bat assembleDebug
   ```
   O APK será gerado em: `app\build\outputs\apk\debug\app-debug.apk`

4. **Regra Obrigatória para Atualizações:**
   Ao criar novas versões, **sempre incremente** o `versionCode` em `app/build.gradle.kts`:
   - Próxima versão: `versionCode = 4`, `versionName = "1.0.3"`
   - Isso garante atualização direta no Android sem sobrescrever os dados locais do usuário.

---

## 📂 Estrutura de Diretórios Chave

```text
app/src/main/java/com/clockinreminder/app/
├── data/
│   ├── local/            # Database Room, DAOs e Entidades
│   └── repository/       # Implementação do repositório
├── domain/
│   ├── engine/           # Motores de cálculo (CalculationEngine, ExitCalculator, InconsistencyDetector)
│   └── model/            # Modelos de domínio imutáveis
├── export/               # Exportadores PDF, Excel, CSV e BackupManager
├── notifications/        # AlarmManager e BroadcastReceivers
└── ui/
    ├── components/       # Diálogos, Cards e componentes Compose reutilizáveis
    ├── navigation/       # Navegação Jetpack Compose
    ├── screens/          # Telas (Home, Calendário, Relatórios, Configurações, Onboarding, etc.)
    └── theme/            # Tema Dark, tipografia e paleta de cores
```

---

## 📋 Prompt Inicial Sugerido para o Antigravity no PC Pessoal

Ao iniciar a conversa no computador pessoal, você pode enviar o seguinte comando para o seu Antigravity:

> "Clonei o projeto do repositório https://github.com/PedroGonzalesDev/Clock-in-Reminder.git. Leia o arquivo `PROJECT_CONTEXT_ANTIGRAVITY.md` na raiz do projeto para entender a arquitetura, regras de cálculo PJ/CLT e configurações de assinatura. Aguardo suas orientações para as próximas melhorias."
