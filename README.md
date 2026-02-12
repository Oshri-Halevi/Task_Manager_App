# Task Manager App 📋

Android Task Management Application built with Kotlin and MVVM architecture.

## 🚀 Features
- User authentication (Google Sign-In & Supabase)
- Create, edit and delete tasks
- Lists management
- Google Calendar integration
- Local storage using Room
- Remote sync with Supabase
- Offline support with FakeRemote fallback
- Dark mode support
- Hebrew & English localization

## 🏗 Architecture
- MVVM
- Repository Pattern
- Single Activity + Multiple Fragments
- Navigation Component
- ViewBinding

## 🛠 Tech Stack
- Kotlin
- Android Jetpack
- Room
- LiveData & ViewModel
- Supabase (Auth + PostgREST)
- Ktor (OkHttp engine)
- Google Sign-In SDK
- Google Calendar API
- Coroutines

## 🔐 Configuration

Create a `local.properties` file with:
SUPABASE_URL=your_supabase_url
SUPABASE_ANON_KEY=your_supabase_anon_key
GOOGLE_WEB_CLIENT_ID=your_google_web_client_id

If not provided, the app will fallback to FakeRemote.



Developed by Oshri Halevi, Matanel Hofman, David Kitunov
