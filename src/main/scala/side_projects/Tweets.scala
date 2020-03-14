package side_projects

case class Author(handle: String)

case class Hashtag(name: String)

case class Tweet(author: Author, timestamp: Long, body: String) {
    
    def hashtags: Set[Hashtag] =
        body
            .split(" ")
            .collect {
                case t if t.startsWith("#") => Hashtag(t.replaceAll("[^#\\w]", ""))
            }
            .toSet
}

object Author {
    
    def apply(handle: String): Author = new Author(handle)
}

object Hashtag {
    
    def apply(name: String): Hashtag = new Hashtag(name)
}

object Tweet {
    
    def apply(author: Author, timestamp: Long, body: String): Tweet = new Tweet(author, timestamp, body)
}
