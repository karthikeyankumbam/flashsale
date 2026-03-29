export function getUserId(): string {
  const key = 'flashsale_user_id';
  let id = localStorage.getItem(key);
  if (!id) {
    id = 'U' + Math.floor(1000 + Math.random() * 9000);
    localStorage.setItem(key, id);
  }
  return id;
}