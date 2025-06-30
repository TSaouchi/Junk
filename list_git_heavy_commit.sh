git rev-list --objects --all | \
  git cat-file --batch-check='%(objecttype) %(objectname) %(objectsize) %(rest)' | \
  sed -n 's/^blob //p' | \
  sort -k2nr -t' ' | \
  head -20 | \
  while read size sha path; do
    commit_info=$(git log --all --pretty=format:"%H %ad" --date=short --diff-filter=A --find-object=$sha | tail -1)
    echo -e "$size\t$sha\t$path\t$commit_info"
  done
