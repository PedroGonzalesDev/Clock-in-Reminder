# Clock in Reminder (Ponto Eletrônico Inteligente)

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-green.svg)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-34-blue.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-teal.svg)](https://developer.android.com/jetpack/compose)
[![Database](https://img.shields.io/badge/SQLite-Room%202.6.1-orange.svg)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-Proprietary-lightgrey.svg)]()

> **Clock in Reminder** é um aplicativo Android nativo, 100% offline, desenvolvido com arquitetura robusta e design de instrumento profissional para controle rigoroso de jornada de trabalho, cálculo preciso de banco de horas, adicionais (noturno, horas extras a 50%, 60% e 100%), calculadora de horário de saída, detector de inconsistências e exportação de relatórios em PDF, Excel (XML) e CSV.

O projeto foi concebido e validado diretamente com base na planilha de referência de horas de Pedro (`10 - Planilha Horas Pedro.xlsx`), garantindo estrita conciliação matemática e legal (CLT).

---

## 📱 APK Instalável

O executável compilado pronto para instalação em dispositivos físicos ou emuladores encontra-se em:

```text
c:\Users\pedro.gonzales\Documents\Projetos\clockinreminder\app\build\outputs\apk\debug\app-debug.apk
```

### Como Instalar no Android:

#### Método 1: Via ADB (Desenvolvedor / Terminal)
Com o dispositivo conectado via cabo USB (ou emulador ativo) e a "Depuração USB" ativada:
```powershell
adb install -r "c:\Users\pedro.gonzales\Documents\Projetos\clockinreminder\app\build\outputs\apk\debug\app-debug.apk"
```

#### Método 2: Transferência Direta (WhatsApp, Google Drive ou Cabo)
1. Copie o arquivo `app-debug.apk` para o seu celular (via cabo USB, envio para si mesmo no WhatsApp, Google Drive, OneDrive, etc.).
2. No celular, abra o arquivo `app-debug.apk` através do gerenciador de arquivos ou app de mensagens.
3. Se o Android exibir o alerta *"Para sua segurança, seu smartphone não tem permissão para instalar apps desconhecidos desta fonte"*, toque em **Configurações** e ative **Permitir desta fonte**.
4. Toque em **Instalar** e em seguida **Abrir**.

---

## ✨ Principais Funcionalidades

### 1. Registro Rápido & Contextual (Home)
- **Relógio digital em tempo real** com segundos e data completa em português (`dd/MM/yyyy`).
- **Botão Contextual de Alta Visibilidade**: Detecta automaticamente o próximo estágio da jornada (`Entrada 1`, `Saída Intervalo`, `Volta Intervalo`, `Saída Fim de Turno`, `Entrada Extra`, etc.).
- **Cronômetro de Turno Ativo**: Exibe a duração do expediente corrente em contagem ao vivo.
- **3 Badges de KPI Diário**: Horas Trabalhadas Hoje, Saldo do Dia (`+` ou `-`) e Horas Noturnas computadas.
- **Linha do Tempo Visual**: Histórico das batidas do dia com badges de tipo, horário exato, indicador de edição/manual e botão de exclusão.
- **Ações Rápidas**: Batida retroativa manual com justificativa obrigatória e classificação rápida do dia (Normal, Feriado, Folga DSR, Atestado Médico, Compensação).

### 2. Motor de Cálculo de Horas & Adicionais (Engine CLT)
- **Conciliação Física Perfeita**: `Horas Normais + HE 50% + HE 60% + HE 100% == Horas Trabalhadas`. Nunca há distorção ou perda de minutos.
- **Hora Noturna Reduzida CLT**: Período entre 22:00 e 05:00 é computado com a redução ficta legal de 52 minutos e 30 segundos (fator $60 / 52.5 = 1,142857$). 7 horas de relógio correspondem a 8 horas fictas.
- **Adicional Noturno Customizável**: Padrão de 30% conforme política de Pedro (CLT base de 20% com suporte a acordos sindicais).
- **Faixas de Horas Extras**:
  - Dias úteis: 50% (ou banco de horas conforme configuração ativa).
  - Sábados: 60% (conforme planilha de Pedro).
  - Domingos e Feriados: 100%.
- **Cálculo Financeiro em Centavos Inteiros**: Base de Pedro a `R$ 25,74/h`, hora noturna calculada com 30% (`R$ 33,46/h`), sábados 60% (`R$ 41,18/h`) e domingos 100% (`R$ 51,48/h`), eliminando erros de arredondamento de ponto flutuante.

### 3. Calculadora Preditiva de Horário de Saída
Permite ao trabalhador saber com precisão de minutos o momento exato em que deve bater o ponto de saída:
- **Cenário 1: Cumprir Jornada Exata**: Calcula o término da carga horária prevista (ex: 9h00) abatendo o intervalo real já realizado.
- **Cenário 2: Fazer 30 Minutos Extras**: Saída programada para gerar exatamente +00:30 de crédito.
- **Cenário 3: Fazer 1 Hora Extra**: Saída programada para gerar exatamente +01:00 de crédito.
- **Cenário 4: Zerar Saldo Negativo Acumulado**: Projeta o horário necessário para pagar o déficit do banco de horas.
- **Cenário 5: Usar Crédito do Banco**: Permite saída antecipada sem penalização, consumindo o saldo positivo existente.
- **Projeção para os Próximos 5 Dias Úteis**: Horários recomendados de saída com base na média histórica e metas da semana.

### 4. Detector Proativo de Inconsistências
Varredura inteligente nos registros para blindar o trabalhador contra esquecimentos e erros:
- ⚠️ Batidas duplicadas (intervalo inferior a 2 minutos).
- ⚠️ Batidas consecutivas de mesmo tipo (ex: duas Entradas seguidas sem Saída).
- ⚠️ Turno aberto sem encerramento no final do dia.
- ⚠️ Turno com duração superior a 14 horas ininterruptas.
- ⚠️ Intervalo de almoço inferior a 1 hora ou inexistente em jornadas > 6 horas.
- ⚠️ Dias úteis passados sem qualquer registro de ponto nem justificativa de ausência.
- 🔧 **Resolução em 1 Clique**: Ações contextuais para "Fechar Turno", "Excluir Duplicada", "Justificar Falta" ou "Adicionar Intervalo".

### 5. Relatórios & Exportação Multi-Formato
- **PDF Profissional Nativo (A4)**:
  - Gerado via `android.graphics.pdf.PdfDocument` nativo do SO (sem dependências externas vulneráveis).
  - Cabeçalho institucional, dados do colaborador e período apurado.
  - Tabela dia a dia com Entrada 1, Saída 1, Entrada 2, Saída 2, Intervalo, Horas Trabalhadas, Horas Noturnas, Saldo do Dia e Ocorrência.
  - Quadro resumo financeiro completo (Horas Normais, Adicional Noturno, HE 50%, HE 60%, HE 100% e Total Bruto em R$).
  - Termo de declaração e aviso legal de conformidade com a Portaria 671 do MTE.
- **Excel (.xml Spreadsheet Format)**:
  - XML compatível com Microsoft Excel, Google Planilhas e LibreOffice Calc.
  - Preserva formatação tipográfica, cabeçalhos estilizados, larguras de colunas e fórmulas somatórias.
- **CSV Brasileiro (.csv)**:
  - Separador ponto e vírgula (`;`), codificação UTF-8 com Byte Order Mark (BOM) para abertura imediata sem caracteres corrompidos no Excel em português.
- **Compartilhamento Direto**: Intent nativa `ACTION_SEND` compatível com WhatsApp, Telegram, E-mail, Google Drive e salvamento no armazenamento do dispositivo.

### 6. Calendário & Gestão Mensal
- Visão mensal com marcadores coloridos de status por dia (Completo, Incompleto, Hora Extra, Banco Negativo, Feriado, Atestado, Folga).
- Bottom sheet instantâneo ao clicar em qualquer dia para edição, visualização das batidas e inclusão de observações.

### 7. Gráficos & Analytics de Produtividade
- Gráfico de barras nativo em Jetpack Compose com histórico de horas diárias.
- Linha de meta da jornada contratual para rápida visualização de sobrejornada ou déficit.
- Indicadores de pontualidade, dias trabalhados no mês, média diária de horas e total de adicionais.

### 8. Lembretes & Notificações Inteligentes
- Agendamento via `AlarmManager` exato para avisar o colaborador 10 minutos antes da entrada e do término do intervalo de almoço.
- Alerta proativo de "Turno Aberto" caso passe do horário previsto sem o registro da saída.

### 9. Backup, Restauração & Auditoria Local
- Exportação completa dos dados para arquivo JSON local.
- Restauração com integridade referencial protegida.
- Trilha de auditoria interna: alterações manuais preservam carimbo de data/hora e motivo.

---

## 🏗️ Arquitetura do Software

O projeto segue estritamente os princípios de **Clean Architecture**, **SOLID** e padrão **MVVM** recomendados pelo Google:

```text
com.clockinreminder/
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt          # Banco SQLite Room com seed de feriados nacionais
│   │   ├── Converters.kt           # Conversores para LocalDate, LocalTime e Enums
│   │   ├── Daos.kt                 # Data Access Objects com Flow reativo
│   │   └── Entities.kt             # Tabelas: Profile, Schedule, Punch, Occurrences, etc.
│   └── repository/
│       ├── ClockInRepository.kt     # Interface de repositório da camada de domínio
│       └── ClockInRepositoryImpl.kt # Implementação com injeção de DAOs e transações
├── domain/
│   ├── engine/
│   │   ├── CalculationEngine.kt    # Motor determinístico de cálculo de horas, CLT e R$
│   │   ├── ExitCalculator.kt       # Calculadora preditiva de horários de saída
│   │   └── InconsistencyDetector.kt# Detector proativo de erros e anomalias
│   └── model/                      # Modelos puros de dados e regras
├── export/
│   ├── BackupManager.kt            # Exportador/Importador de backup JSON
│   ├── CsvExporter.kt              # Exportador CSV UTF-8 BOM com separador ';'
│   ├── ExcelExporter.kt            # Exportador Spreadsheet XML com estilos e fórmulas
│   └── PdfExporter.kt                # Exportador de PDF A4 multipáginas nativo
├── notification/
│   ├── ReminderReceiver.kt         # BroadcastReceiver de alarmes e notificações
│   └── ReminderScheduler.kt        # Agendador inteligente via AlarmManager
├── ui/
│   ├── components/
│   │   ├── CommonComponents.kt      # Cards de KPI, botão contextual de batida, timeline
│   │   └── Dialogs.kt               # Diálogos de batida manual e classificação de dia
│   ├── navigation/
│   │   ├── AppNavigation.kt        # Scaffold com BottomNavigationBar e Navigation Host
│   │   └── Screen.kt               # Rotas e ícones do app
│   ├── screens/
│   │   ├── analytics/              # Dashboard gráfico de produtividade
│   │   ├── calendar/               # Calendário interativo mensal
│   │   ├── calculator/             # Calculadora de horário de saída
│   │   ├── home/                   # Tela principal de registro e status
│   │   ├── inconsistencies/        # Central de pendências e anomalias
│   │   ├── onboarding/             # Fluxo de boas-vindas e configuração inicial
│   │   ├── reports/                # Tela de exportação e visualização de holerite
│   │   └── settings/               # Configurações de jornada, tarifas e banco
│   └── theme/
│       ├── Color.kt                # Paleta "Instrumento Profissional" (Slate, Sand, Petrol, Copper)
│       ├── Theme.kt                # Suporte a Tema Escuro / Claro Material 3
│       └── Type.kt                 # Tipografia com numerais tabulares monoespaçados
├── ClockInApp.kt                   # Application class para inicialização de canais
└── MainActivity.kt                 # Activity única com Compose Edge-to-Edge
```

---

## 📊 Regras de Negócio e Reconciliação com a Planilha de Pedro

| Regra / Parâmetro | Implementação no Clock in Reminder | Planilha de Referência (`10 - Planilha Horas Pedro.xlsx`) |
| :--- | :--- | :--- |
| **Jornada Padrão** | 9 horas diárias (Segunda a Sexta) | 9:00 horas diárias |
| **Pontos por Dia** | 4 batidas: Entrada 1, Saída 1, Entrada 2, Saída 2 | Colunas E1, S1, E2, S2 |
| **Valor Hora Base** | `R$ 25,74` (calculado em centavos: 2574) | Taxa horária: `R$ 25,74` |
| **Adicional Noturno**| +30% entre 22:00 e 05:00 (`R$ 33,46/h`) | Adicional Noturno: 30% (`R$ 33,46`) |
| **Hora Ficta Noturna**| Fator legal CLT: 52m 30s ($60 / 52.5$) | Redução legal da hora noturna |
| **Sábados** | Adicional de 60% (`R$ 41,18/h`) | Sábado 60%: `R$ 41,18` |
| **Domingos e Feriados**| Adicional de 100% (`R$ 51,48/h`) | Domingo 100%: `R$ 51,48` |
| **Feriados Nacionais**| Pre-carregados automaticamente no banco | Considera feriados vigentes |
| **Privacidade** | 100% Offline, dados gravados apenas no aparelho | Arquivo local seguro |

---

## 🧪 Testes Automatizados

O projeto conta com suíte completa de testes unitários automatizados validando o motor de cálculo, o detector de inconsistências e a calculadora de horário de saída.

Para executar os testes:
```powershell
.\gradlew.bat testDebugUnitTest
```

Resultado da execução:
```text
BUILD SUCCESSFUL in 14s
26 actionable tasks: 26 executed
```
- `CalculationEngineTest`: Valida conciliação de jornada normal, horas extras a 50%, 60% e 100%, cálculo de hora noturna com redução CLT de 52m30s, horas negativas, tolerâncias e cálculo financeiro em centavos.
- `ExitCalculatorTest`: Valida os 5 cenários preditivos de saída, saídas antecipadas com banco positivo e compensação de banco negativo.
- `InconsistencyDetectorTest`: Valida detecção de batidas duplicadas, ausência de intervalo, turnos > 14h e faltas não justificadas.

---

## 🛠️ Requisitos de Compilação

Caso deseje recompilar o código-fonte:
- **JDK:** Java 17 ou Java 21 LTS
- **Gradle:** 8.9 (incluso via Gradle Wrapper)
- **Android SDK:** API 34 (Android 14)
- **Build Tools:** 34.0.0

Comando de compilação:
```powershell
.\gradlew.bat assembleDebug
```

O APK resultante é gerado em `app/build/outputs/apk/debug/app-debug.apk`.

---

## 🔒 Segurança e Privacidade

- **Zero Telemetria:** O aplicativo não conecta a nenhum servidor externo e não envia dados para a nuvem.
- **Armazenamento Privado:** Todos os dados residem no banco SQLite local seguro do Android (`Room`), inacessível por outros aplicativos sem permissão explícita.
- **Exportação Sob Controle do Usuário:** Somente quando o usuário clica ativamente em exportar/compartilhar é que relatórios são gerados e compartilhados via `FileProvider` seguro do Android.
