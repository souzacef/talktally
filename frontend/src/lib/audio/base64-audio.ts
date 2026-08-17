export interface ObjectUrlAudio {
  url: string
  revoke: () => void
}

export function createAudioObjectUrl(contentType: string, base64: string): ObjectUrlAudio {
  const binary = atob(base64)
  const bytes = new Uint8Array(binary.length)
  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index)
  }
  const url = URL.createObjectURL(new Blob([bytes], { type: contentType }))
  return {
    url,
    revoke: () => URL.revokeObjectURL(url),
  }
}
