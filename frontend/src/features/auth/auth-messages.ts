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
  displayName: 'Display name',
  passwordHelp: '10–128 characters with at least one letter and one digit.',
  passwordInvalid: 'Password must contain 10 to 128 characters, including a letter and a digit.',
  creatingAccount: 'Creating account…',
  alreadyRegistered: 'Already registered?',
  unableToRegister: 'Unable to register',
  builtBy: 'Built by Carlos Eduardo Freire de Souza',
  github: 'GitHub',
  backendHealth: 'Backend health',
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
  displayName: 'Nome de exibição',
  passwordHelp: '10–128 caracteres, com pelo menos uma letra e um número.',
  passwordInvalid: 'A senha deve ter de 10 a 128 caracteres, incluindo uma letra e um número.',
  creatingAccount: 'Criando conta…',
  alreadyRegistered: 'Já possui cadastro?',
  unableToRegister: 'Não foi possível criar a conta',
  builtBy: 'Criado por Carlos Eduardo Freire de Souza',
  github: 'GitHub',
  backendHealth: 'Status do backend',
}

const messages: Record<AppLocale, Record<AuthMessageKey, string>> = {
  'en-US': enUS,
  'pt-BR': ptBR,
}

export function authText(locale: AppLocale, key: AuthMessageKey): string {
  return messages[locale][key]
}
