export const materialTypes = [
  {type:'Fotografia', label:'Fotografias', icon:'photo'},
  {type:'Documento', label:'Documentos', icon:'document'},
  {type:'Entrevista', label:'Entrevistas', icon:'microphone'},
  {type:'Áudio', label:'Áudios', icon:'audio'},
  {type:'Vídeo', label:'Vídeos', icon:'video'},
  {type:'Relato', label:'Relatos', icon:'book'},
  {type:'Outro', label:'Outros', icon:'folder'}
] as const;

export function materialIcon(type: string) {
  return materialTypes.find(item => item.type === type)?.icon ?? 'folder';
}
