import type { AppLocale } from '@/app/providers/locale-provider'

const enUS = {
  welcomeBack: 'Welcome to TalkTally!',
  loginSubtitle: 'Sign in to understand your money.',
  registrationComplete: 'Registration complete. You can sign in now.',
  email: 'Email',
  password: 'Password',
  signingIn: 'Signing in…',
  signIn: 'Sign in',
  newToTalkTally: 'New to TalkTally?',
  createAccount: 'Create an account',
  unableToSignIn: 'Unable to sign in',
  createYourAccount: 'Create your account',
  registerSubtitle: 'A calmer way to understand your money.',
  displayName: 'Name',
  confirmPassword: 'Confirm password',
  passwordsDoNotMatch: 'Passwords do not match.',
  showPassword: 'Show password',
  hidePassword: 'Hide password',
  showConfirmPassword: 'Show confirm password',
  hideConfirmPassword: 'Hide confirm password',
  passwordHelp: '10–128 characters with at least one letter and one digit.',
  passwordInvalid: 'Password must contain 10 to 128 characters, including a letter and a digit.',
  creatingAccount: 'Creating account…',
  alreadyRegistered: 'Already registered?',
  unableToRegister: 'Unable to register',
  builtBy: 'Built by Carlos Eduardo Freire de Souza',
  github: 'GitHub',
  backendHealth: 'Backend health',
  backendChecking: 'Checking backend status…',
  backendUp: 'Backend is up.',
  backendUnavailable: 'Backend is unavailable or still waking up.',
  retryBackend: 'Retry',
} as const

type AuthMessageKey = keyof typeof enUS

const ptBR: Record<AuthMessageKey, string> = {
  welcomeBack: 'Boas-vindas ao TalkTally!',
  loginSubtitle: 'Entre para entender melhor suas finanças.',
  registrationComplete: 'Cadastro concluído. Você já pode entrar.',
  email: 'E-mail',
  password: 'Senha',
  signingIn: 'Entrando…',
  signIn: 'Entrar',
  newToTalkTally: 'Novo no TalkTally?',
  createAccount: 'Criar uma conta',
  unableToSignIn: 'Não foi possível entrar',
  createYourAccount: 'Crie sua conta',
  registerSubtitle: 'Uma forma mais tranquila de entender seu dinheiro.',
  displayName: 'Nome',
  confirmPassword: 'Confirmar senha',
  passwordsDoNotMatch: 'As senhas não coincidem.',
  showPassword: 'Mostrar senha',
  hidePassword: 'Ocultar senha',
  showConfirmPassword: 'Mostrar confirmação da senha',
  hideConfirmPassword: 'Ocultar confirmação da senha',
  passwordHelp: '10–128 caracteres, com pelo menos uma letra e um número.',
  passwordInvalid: 'A senha deve ter de 10 a 128 caracteres, incluindo uma letra e um número.',
  creatingAccount: 'Criando conta…',
  alreadyRegistered: 'Já possui cadastro?',
  unableToRegister: 'Não foi possível criar a conta',
  builtBy: 'Criado por Carlos Eduardo Freire de Souza',
  github: 'GitHub',
  backendHealth: 'Status do backend',
  backendChecking: 'Verificando o status do backend…',
  backendUp: 'O backend está disponível.',
  backendUnavailable: 'O backend está indisponível ou ainda está iniciando.',
  retryBackend: 'Tentar novamente',
}

const messages: Record<AppLocale, Record<AuthMessageKey, string>> = {
  'en-US': enUS,
  'pt-BR': ptBR,
}

export function authText(locale: AppLocale, key: AuthMessageKey): string {
  return messages[locale][key]
}
