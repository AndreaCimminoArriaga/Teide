# Teide

One Paragraph of project description goes here

## Building Teide 

Download this project and open it  into you IDE, then compile as a runnable jar the class 'TeideMain.jar' as teide.jar

## Getting Started

Having the teide.jar you only need to pass as argument an input file with the setup of the algorithm:

```
java -jar teide.jar input-file.json
```


The input file is a json file that must contain the following data:







## Running the tests

We have inlcuded two scenarios with their respective datasets (as TDB jena databases) and input files to test Teide. The scenarios are 'Restaurants' and 'Acm-Newcastle', to run each:

```
java -jar teide.jar restaurants-file.json
```

```
java -jar teide.jar rae-newcastle-file.json
```


## Authors

* **Andrea Cimmino** - *Initial work* - [PurpleBooth](http://www.tdg-seville.info/acimmino/Home)


## License

This project is licensed under the TDG's License - visit http://www.tdg-seville.info/license.html for details

## Acknowledgments

* Spanish R&D programme (grants TIN2013-40848-R and TIN2013-40848-R)
