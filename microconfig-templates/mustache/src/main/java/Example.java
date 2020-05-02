import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

public class Example {
    List<Item> items() {
        return asList(
                new Item("Item 1", "$149.99", asList(new Feature("New!"), new Feature("Awesome!"))),
                new Item("Item 2", "$29.99", asList(new Feature("Old."), new Feature("Ugly.")))
        );
    }

  /*
   items:
       - {name: Alex, price: 100}
       - {name: Bob, price: 50}
   */

  /**
   items[0].name: Alex
   items[0].price: 100
   items[1].name: 100
   items[2].price: 100
   */

  static class Item {
        Item(String name, String price, List<Feature> features) {
            this.name = name;
            this.price = price;
            this.features = features;
        }

        String name, price;
        List<Feature> features;
    }

    static class Feature {
        Feature(String description) {
            this.description = description;
        }

        String description;
    }

    public static void main(String[] args) throws IOException {
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile("repo.mustache");

        Map load = new Yaml().load(
                "some:\n" +
                "  repo: [\n" +
                        "    { name: resque },\n" +
                        "    { name: hub },\n" +
                        "    { name: rip }\n" +
                        "  ]");
        Writer execute = mustache.execute(new PrintWriter(System.out), load);
        execute.flush();
    }
}