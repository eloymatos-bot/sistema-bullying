export function requiredText(value: string, label: string, max: number): string {
  const text = value.trim();
  if (!text) return `Completa ${label}.`;
  return text.length > max ? `El máximo para ${label} es de ${max} caracteres.` : '';
}

export function personName(value: string, label: string): string {
  const error = requiredText(value, label, 100);
  if (error) return error;
  return /\p{L}/u.test(value) && /^[\p{L}\p{M} .,'’ʻʼ·-]+$/u.test(value.trim())
    ? '' : 'Usa letras, espacios, puntos, guiones o apóstrofos.';
}

export function studentFields(names: string, surnames: string, grade: number, section: string) {
  return {
    names: personName(names, 'los nombres'),
    surnames: personName(surnames, 'los apellidos'),
    grade: [1, 2, 3, 4, 5].includes(grade) ? '' : 'Elige un grado de 1.º a 5.º.',
    section: requiredText(section.toUpperCase(), 'la sección', 10) ||
      (/^[\p{L}\p{N}][\p{L}\p{M}\p{N} -]*$/u.test(section.trim()) ? '' : 'Usa una sección como A, Única o 2-A.')
  };
}

export function emailError(value: string): string {
  const email = value.trim();
  if (!email) return 'Ingresa tu correo.';
  return email.length <= 254 && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
    ? '' : 'Escribe un correo completo, por ejemplo nombre@institucion.edu.pe.';
}

export function passwordError(password: string, creatingAccount = false): string {
  if (!password.trim()) return 'Ingresa la contraseña.';
  if (creatingAccount && password.length < 12) return 'Usa al menos 12 caracteres.';
  return new TextEncoder().encode(password).length > 72
    ? 'La contraseña supera 72 bytes. Acórtala.' : '';
}

export function isCalendarDate(value: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value) || value.startsWith('0000-')) return false;
  const date = new Date(value + 'T00:00:00Z');
  return Number.isFinite(date.getTime()) && date.toISOString().slice(0, 10) === value;
}

export function hasFieldErrors(fields: Record<string, string>): boolean {
  return Object.values(fields).some(Boolean);
}
