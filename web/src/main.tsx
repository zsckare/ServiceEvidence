import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import { CssBaseline, ThemeProvider, createTheme } from '@mui/material';
import App from './App';
const theme=createTheme({palette:{mode:'light',primary:{main:'#176b76'},background:{default:'#f3f6f9'}},shape:{borderRadius:12},typography:{fontFamily:'Inter, system-ui, sans-serif'}});
ReactDOM.createRoot(document.getElementById('root')!).render(<React.StrictMode><ThemeProvider theme={theme}><CssBaseline/><QueryClientProvider client={new QueryClient({defaultOptions:{queries:{retry:1,staleTime:30000}}})}><BrowserRouter><App/></BrowserRouter></QueryClientProvider></ThemeProvider></React.StrictMode>);
