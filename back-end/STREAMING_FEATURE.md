# Live Code Streaming Feature

## Overview

The application now supports **real-time streaming** of LLM-generated code. You can watch code being written live as the Qwen2.5-Coder model generates it!

## Features

✅ **Server-Sent Events (SSE)** for real-time streaming
✅ **Live code display** - see code appear character by character
✅ **Auto-scroll** - automatically scrolls to show latest code
✅ **Visual indicators** - "LIVE" badge and loading spinner during generation
✅ **Automatic saving** - code is saved to database when streaming completes

## New Endpoints

### Backend Streaming
```
GET /api/projects/{id}/generate/backend/stream
Content-Type: text/event-stream
```

### Frontend Streaming
```
GET /api/projects/{id}/generate/frontend/stream
Content-Type: text/event-stream
```

## How It Works

1. **User clicks "Generate Backend (Live)" or "Generate Frontend (Live)"**
2. **Frontend opens SSE connection** to backend
3. **Backend connects to Ollama** with streaming enabled
4. **Ollama streams code chunks** back to backend
5. **Backend forwards chunks** to frontend via SSE
6. **Frontend displays code** in real-time as it arrives
7. **Code is saved** to database when complete

## Frontend UI Changes

- **New buttons**: "Generate Backend (Live)" and "Generate Frontend (Live)"
- **Live indicator**: Shows "LIVE" badge and spinner during generation
- **Real-time display**: Code appears as it's generated
- **Auto-scroll**: Automatically scrolls to show latest code

## Technical Details

### Backend
- Uses `SseEmitter` for Server-Sent Events
- `StreamingCodeGenerationService` handles Ollama streaming
- Ollama streaming enabled with `stream: true`
- Code chunks forwarded via SSE events named "code-chunk"
- Completion event named "complete"

### Frontend
- Uses native `EventSource` API for SSE
- Token passed as query parameter (EventSource limitation)
- Handles multiple event types: `code-chunk`, `complete`, `error`
- Auto-scrolls to bottom as code arrives

## Authentication

SSE endpoints require authentication. Token is passed via:
- Query parameter: `?token=YOUR_JWT_TOKEN` (for EventSource)
- Or via Authorization header (if using fetch API)

The JWT filter has been updated to check query parameters for SSE endpoints.

## Usage

1. **Create a project** with your requirements prompt
2. **Click "Generate Backend (Live)"** or **"Generate Frontend (Live)"**
3. **Watch the code appear** in real-time!
4. **Code is automatically saved** when generation completes

## Benefits

- **Better UX**: See progress in real-time instead of waiting
- **Transparency**: Watch exactly what the LLM is generating
- **Engaging**: More interactive and satisfying experience
- **Debugging**: Can see if generation is stuck or working

## Troubleshooting

### Streaming doesn't start
- Check browser console for errors
- Verify backend is running and accessible
- Check Ollama is running on VPS
- Verify authentication token is valid

### Code stops streaming
- Check backend logs for Ollama errors
- Verify Ollama model is loaded
- Check network connection
- May need to restart streaming

### Authentication errors
- Token may have expired - try logging in again
- Check token is being passed correctly
- Verify CORS configuration allows SSE

## Future Enhancements

- WebSocket support for bidirectional communication
- Syntax highlighting for generated code
- Code formatting/beautification
- Download generated code as files
- Multiple language support

