const root = document.documentElement;
const menuButton = document.querySelector(".menu-toggle");
const sidebar = document.querySelector(".sidebar");
const themeButton = document.querySelector(".theme-toggle");
const searchInput = document.querySelector("#docs-search");
const searchResults = document.querySelector("#search-results");
const navigation = document.querySelector("#docs-navigation");

menuButton?.addEventListener("click", () => {
  const open = sidebar.classList.toggle("is-open");
  menuButton.setAttribute("aria-expanded", String(open));
});

themeButton?.addEventListener("click", () => {
  const current = root.dataset.theme ?? (matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light");
  const next = current === "dark" ? "light" : "dark";
  root.dataset.theme = next;
  localStorage.setItem("jade-docs-theme", next);
});

let pagesPromise;
const getPages = () => {
  pagesPromise ??= fetch(searchResults.dataset.indexUrl).then((response) => {
    if (!response.ok) throw new Error(`Search index request failed: ${response.status}`);
    return response.json();
  });
  return pagesPromise;
};

searchInput?.addEventListener("input", async () => {
  const query = searchInput.value.trim().toLocaleLowerCase();
  if (query.length < 2) {
    searchResults.hidden = true;
    navigation.hidden = false;
    searchResults.replaceChildren();
    return;
  }

  const pages = await getPages();
  if (query !== searchInput.value.trim().toLocaleLowerCase()) return;
  const matches = pages
    .filter((page) => `${page.title} ${page.text}`.toLocaleLowerCase().includes(query))
    .slice(0, 12);

  const list = document.createElement("ul");
  for (const page of matches) {
    const item = document.createElement("li");
    const link = document.createElement("a");
    link.href = page.url;
    link.textContent = page.title;
    item.append(link);
    list.append(item);
  }

  if (!matches.length) {
    const empty = document.createElement("p");
    empty.textContent = "No results";
    searchResults.replaceChildren(empty);
  } else {
    searchResults.replaceChildren(list);
  }
  searchResults.hidden = false;
  navigation.hidden = true;
});
