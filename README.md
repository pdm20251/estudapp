# Estuda++ 💬

**Aplicativo de estudo com flashcards inteligentes para Android**

Desenvolvido como projeto acadêmico para a disciplina de Programação para Dispositivos Móveis da Universidade Federal de Uberlândia (UFU).

## 👥 Equipe de Desenvolvimento

- **Chloe Anne Scaramal** - 12311BSI232
- **Gabriel Augusto Paiva** - 12311BSI245
- **João Pedro Zanetti** - 12311BSI230
- **Marcelo Gabriel Milani Santos** - 12311BSI251
- **Marcos Antônio da Silva Junior** - 12311BSI256
- **Paulo Daniel Forti da Fonseca** - 12311BSI321
- **Pedro Henrique Lopes Duarte** - 12311BSI237
- **Rayane Reis Mota** - 12311BSI233
- **Vinícius Resende Garcia** - 12021BCC027

## 📦 Entregáveis

Os arquivos entregáveis estão na pasta delivery/ na raiz do repositório:
```
delivery
├── cronograma.pdf
├── estudapp.apk
├── relatório.pdf
├── slides.pdf
├── vídeo-api.mkv
└── vídeo-app.mp4
```

## 📱 Sobre o Projeto

O Estuda++ é um aplicativo nativo para Android que revoluciona o estudo através de flashcards inteligentes com sistema de repetição espaçada. Desenvolvido com foco na experiência do usuário e integração com inteligência artificial, oferece uma plataforma completa para otimização do aprendizado.

## ✨ Funcionalidades Principais

### 🔐 Autenticação e Perfil
- Login e cadastro via **email/senha**
- Perfis de usuário personalizáveis
- Gerenciamento de sessões seguras

### 📚 Sistema de Flashcards Inteligentes
- **Quatro tipos de flashcards**:
  - **Frente/Verso**: Cards tradicionais para memorização
  - **Cloze**: Preenchimento de lacunas para fixação
  - **Digite a Resposta**: Resposta livre com validação por IA
  - **Múltipla Escolha**: Questões objetivas com alternativas
- **Sistema de Repetição Espaçada** baseado em algoritmos científicos
- Suporte a **mídia** (imagens e áudio) nos flashcards
- **Geração automática** de flashcards via MonitorIA

### 🧠 MonitorIA - Assistente de Estudos com IA
- **Chat inteligente** para tirar dúvidas sobre qualquer assunto
- **Geração automática de flashcards** baseada em prompts
- **Validação inteligente** de respostas abertas
- Integração com backend em Node.js e Google Generative AI

### 📊 Sistema de Estatísticas Avançado
- **Análise de desempenho** por deck e por localização
- **Visualização de progresso** com gráficos interativos
- **Controle de sessões** de estudo com scoring detalhado
- **Repetição espaçada** com cálculo automático de próximas revisões

### 📍 Geolocalização Inteligente
- **Locais favoritos** para estudo com geofencing
- **Notificações automáticas** ao entrar/sair de locais de estudo
- **Estatísticas por localização** para análise de produtividade
- Integração com Google Maps

### 🔗 Compartilhamento e Colaboração
- **Compartilhamento de decks** via links únicos
- Sistema de **importação** de decks compartilhados
- **API RESTful** para integração e expansão

## 🛠️ Tecnologias Utilizadas

### Frontend (Android)
- **Linguagem**: Kotlin/Java
- **UI Framework**: Jetpack Compose
- **Arquitetura**: MVVM com Repository Pattern
- **Navegação**: Navigation Compose

### Backend e Serviços
- **Firebase**:
  - Realtime Database (armazenamento de dados)
  - Authentication (autenticação)
  - Storage (arquivos de mídia)
- **API Backend**: Node.js hospedada no Google Cloud Run
- **IA**: Google Generative AI (Gemini)

### Bibliotecas Principais
- **Material Design 3** (interface moderna)
- **Google Maps Compose** (mapas integrados)
- **Coil** (carregamento eficiente de imagens)
- **OkHttp** (comunicação HTTP)
- **Accompanist Permissions** (gerenciamento de permissões)

## 📋 Requisitos do Sistema

- Android 8.0 (API 26) ou superior
- Conexão com a Internet
- **Permissões**:
  - Acesso à Internet
  - Localização (opcional, para geofencing)
  - Câmera e galeria (para mídia em flashcards)
  - Notificações (para lembretes de estudo)

## 🚀 Instalação e Configuração

### Para Usuários
1. Baixe o arquivo `estudapp.apk` da pasta `delivery/`
2. Instale o aplicativo no dispositivo Android
3. Crie uma conta ou faça login
4. Comece a criar seus decks de estudo!

### Para Desenvolvedores
1. Clone o repositório
```bash
git clone [URL_DO_REPOSITORIO]
```

2. Abra o projeto no Android Studio

3. Configure o Firebase:
   - Adicione o arquivo `google-services.json` na pasta `app/`
   - Configure as regras de segurança no Firebase Console

4. Configure a API do Google Maps:
   - Adicione sua chave da API no arquivo `local.properties`:
   ```
   MAPS_API_KEY=sua_chave_aqui
   ```

5. Compile e execute o projeto

## 📊 Estrutura do Projeto

```
app/
├── src/main/java/com/example/estudapp/
│   ├── data/model/          # Modelos de dados (DTOs)
│   ├── domain/             # Lógica de negócio e repositórios
│   ├── ui/feature/         # Telas organizadas por funcionalidade
│   │   ├── auth/           # Autenticação
│   │   ├── flashcard/      # Sistema de flashcards
│   │   ├── chat/           # MonitorIA
│   │   ├── location/       # Geolocalização
│   │   └── profile/        # Perfil e estatísticas
│   ├── navigate/           # Navegação entre telas
│   └── ui/theme/           # Tema e cores do app
├── res/
│   ├── drawable/           # Ícones e recursos gráficos
│   ├── layout/             # Layouts XML (legacy)
│   └── values/             # Cores, strings e estilos
└── AndroidManifest.xml
```

## 🎯 Funcionalidades Implementadas

- ✅ **Sistema completo de flashcards** com 4 tipos diferentes
- ✅ **Repetição espaçada** com algoritmo científico
- ✅ **MonitorIA** com chat inteligente e geração de cards
- ✅ **Geolocalização** com geofencing e notificações
- ✅ **Estatísticas avançadas** por deck e localização
- ✅ **Compartilhamento de decks** via API
- ✅ **Interface moderna** com Material Design 3
- ✅ **Suporte a mídia** (imagens e áudio)
- ✅ **Sistema robusto de autenticação**
- ✅ **Arquitetura escalável** e bem organizada

## 🔄 API Backend

O aplicativo conta com uma API RESTful desenvolvida em Node.js que oferece:

- **Geração de flashcards via IA**
- **Validação inteligente de respostas**
- **Chat com MonitorIA**
- **Compartilhamento de decks**
- **Cálculo de repetição espaçada**

### Endpoints Principais
- `POST /decks/{deckId}/flashcards/generate` - Gera flashcards via IA
- `POST /flashcards/validate` - Valida respostas abertas
- `POST /chat/respond` - Chat com MonitorIA
- `GET /share-deck/{userId}/{deckId}` - Compartilha decks
- `POST /decks/{deckId}/calculate-next-review` - Calcula próxima revisão

## 📈 Algoritmo de Repetição Espaçada

O Estuda++ implementa um sistema científico de repetição espaçada que:

1. **Analisa o desempenho** em cada flashcard
2. **Calcula intervalos otimizados** para revisão
3. **Adapta-se ao progresso** individual do usuário
4. **Maximiza a retenção** de conhecimento a longo prazo

## 🎨 Design e Experiência

- **Material Design 3** para interface moderna e consistente
- **Navegação intuitiva** com bottom navigation e deep linking
- **Animações fluidas** para melhor experiência do usuário
- **Modo escuro/claro** (adaptável ao sistema)
- **Responsividade** para diferentes tamanhos de tela
- **Acessibilidade** com suporte a leitores de tela

## 🔒 Segurança e Privacidade

- **Autenticação Firebase** com tokens JWT
- **Validação server-side** de todas as operações
- **Armazenamento seguro** de dados sensíveis
- **Criptografia** de comunicação (HTTPS)
- **Controle de acesso** baseado em usuário

## 📄 Licença

Este projeto está licenciado sob a GNU General Public License v3.0 - veja o arquivo [LICENSE](LICENSE) para detalhes.

## 🎓 Contexto Acadêmico

Projeto desenvolvido para a disciplina de **Programação para Dispositivos Móveis** da **Universidade Federal de Uberlândia**, orientado pelo professor **Alexsandro Santos Soares**.

**Valor**: 25 pontos  
**Data de Apresentação**: 18/09/2025
