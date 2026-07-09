import fs from 'fs';
import * as compiler from '@vue/compiler-sfc';

const content = fs.readFileSync('src/pages/ProjectDetailPage.vue', 'utf8');
const { descriptor, errors } = compiler.parse(content);

if (errors && errors.length > 0) {
  console.log("Compile Errors:");
  errors.forEach(err => console.log(err.toString()));
} else {
  console.log("No parsing errors found in SFC descriptor.");
  if (descriptor.template) {
    const result = compiler.compileTemplate({
      source: descriptor.template.content,
      filename: 'ProjectDetailPage.vue',
      id: 'xxx'
    });
    if (result.errors && result.errors.length > 0) {
      console.log("Template compile errors:");
      result.errors.forEach(err => console.log(err));
    } else {
      console.log("Template compiled successfully!");
    }
  }
}
