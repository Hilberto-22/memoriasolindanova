// Restrict post-login destinations to local administrative routes.
export function adminDestination(value: string | null): string {
  return value && /^\/admin(?:\/|\?|#|$)/.test(value) && !/[\\\r\n]/.test(value)
    ? value
    : '/admin';
}
